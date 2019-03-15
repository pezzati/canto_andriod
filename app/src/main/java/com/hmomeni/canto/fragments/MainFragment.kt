package com.hmomeni.canto.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.crashlytics.android.Crashlytics
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.rcl.MainRclAdapter
import com.hmomeni.canto.entities.Banner
import com.hmomeni.canto.entities.Genre
import com.hmomeni.canto.utils.ViewModelFactory
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.iomain
import com.hmomeni.canto.utils.navigation.ListNavEvent
import com.hmomeni.canto.utils.navigation.PostNavEvent
import com.hmomeni.canto.vms.MainViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_main.*
import timber.log.Timber

class MainFragment : BaseFragment() {
    private lateinit var viewModel: MainViewModel
    private val compositeDisposable = CompositeDisposable()

    private var adapter: MainRclAdapter? = null

    private val genres: MutableList<Genre> = mutableListOf()
    private val banners: MutableList<Banner> = mutableListOf()

    private var dataLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, ViewModelFactory(context!!.app()))[MainViewModel::class.java]
        loadData()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (adapter == null) {
            adapter = MainRclAdapter(banners, genres).also { adapter ->
                adapter.clickPublisher.subscribe {
                    when (it.type) {
                        MainRclAdapter.ClickEvent.Type.BANNER -> {
                            val banner = adapter.banners[it.item]
                            when {
                                banner.contentType == "multi" -> viewModel.navEvents.onNext(ListNavEvent("url_path", 0, banner.title, banner.link))
                                banner.contentType == "redirect" -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(banner.link)))
                            }
                        }
                        MainRclAdapter.ClickEvent.Type.GENRE -> {
                            val pos = it.row - 1
                            val genre = genres[pos]
                            if (it.item == -1) {
                                findNavController().navigate(MainFragmentDirections.actionMainFragmentToListFragment(genre.name, "url_path", genre.filesLink))
                            } else {
                                val post = genre.posts!![it.item]
                                viewModel.navEvents.onNext(PostNavEvent(post))
                            }

                        }
                    }
                }.addTo(compositeDisposable)
            }
            if (dataLoaded) {
                progressBar.visibility = View.GONE
            }
        } else {
            progressBar.visibility = View.GONE
        }

        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        recyclerView.adapter = adapter
        swipeRefresh.setOnRefreshListener {
            loadData()
        }
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    private fun loadData() {
        viewModel.api.getBanners()
                .map { it.data }
                .iomain()
                .doAfterTerminate { progressBar?.visibility = View.GONE }
                .subscribe({
                    banners.clear()
                    banners.addAll(it)
                    adapter?.notifyDataSetChanged()
                }, {
                    Crashlytics.logException(it)
                    Timber.e(it, "Failed loading banners")
                }).addTo(compositeDisposable)

        viewModel.api.getHomeFeed()
                .iomain()
                .doOnSubscribe {
                    swipeRefresh?.isRefreshing = true
                }
                .doAfterTerminate {
                    swipeRefresh?.isRefreshing = false
                    progressBar?.visibility = View.GONE
                    dataLoaded = true
                }
                .subscribe({
                    genres.clear()
                    it.forEach { f ->
                        val genre = Genre(
                                filesLink = f.moreUrl,
                                link = f.moreUrl,
                                name = f.name,
                                posts = f.posts
                        )
                        genres.add(genre)
                    }
                    adapter?.notifyDataSetChanged()
                }, {
                    Crashlytics.logException(it)
                    Timber.e(it, "Failed loading genres")
                }).addTo(compositeDisposable)
    }
}