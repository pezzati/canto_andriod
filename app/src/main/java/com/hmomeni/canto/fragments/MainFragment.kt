package com.hmomeni.canto.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
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

class MainFragment : androidx.fragment.app.Fragment() {
    private lateinit var viewModel: MainViewModel
    private val compositeDisposable = CompositeDisposable()

    private var adapter: MainRclAdapter? = null

    private val genres: MutableList<Genre> = mutableListOf()
    private val banners: MutableList<Banner> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, ViewModelFactory(context!!.app()))[MainViewModel::class.java]
        viewModel.api.getBanners()
                .map { it.data }
                .iomain()
                .subscribe({
                    banners.addAll(it)
                    adapter?.notifyDataSetChanged()
                }, {
                    Timber.e(it, "Failed loading banners")
                }).addTo(compositeDisposable)

        viewModel.api.getHomeFeed()
                .iomain()
                .doAfterTerminate {
                    progressBar?.visibility = View.GONE
                }
                .subscribe({
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
                    Timber.e(it, "Failed loading genres")
                }).addTo(compositeDisposable)
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
                                viewModel.navEvents.onNext(ListNavEvent("url_path", 0, genre.name, genre.filesLink))
                            } else {
                                val post = genre.posts!![it.item]
                                viewModel.navEvents.onNext(PostNavEvent(post))
                            }

                        }
                    }
                }.addTo(compositeDisposable)
            }
        } else {
            progressBar.visibility = View.GONE
        }

        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(recyclerView.context)
        recyclerView.adapter = adapter

    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }
}