package com.hmomeni.canto.fragments

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class MainFragment : Fragment() {
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
                    viewModel.api.getGenres()
                            .map { it.data }
                            .iomain()
                            .doAfterTerminate {
                                progressBar?.visibility = View.GONE
                            }
                            .subscribe({
                                it.forEach { genre ->
                                    viewModel.api
                                            .getGenrePosts(genre.filesLink)
                                            .map { it.data }
                                            .iomain()
                                            .subscribe({
                                                genre.posts = it
                                                genres.add(genre)
                                                adapter?.notifyDataSetChanged()
                                            }, {
                                                Timber.e(it)
                                            }).addTo(compositeDisposable)

                                }
                            }, {
                                Timber.e(it)
                            }).addTo(compositeDisposable)
                }, {
                    Timber.e(it)
                }).addTo(compositeDisposable)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (adapter == null) {
            adapter = MainRclAdapter(banners, genres).also {
                it.clickPublisher.subscribe {
                    when (it.type) {
                        MainRclAdapter.ClickEvent.Type.BANNER -> {
                        }
                        MainRclAdapter.ClickEvent.Type.GENRE -> {
                            val pos = it.row - 1
                            val genre = genres[pos]
                            if (it.item == -1) {
                                viewModel.navEvents.onNext(ListNavEvent("url_path", 0, genre.name, genre.filesLink))
                            } else {
                                viewModel.navEvents.onNext(PostNavEvent(genre.posts!![it.item]))
                            }

                        }
                    }
                }.addTo(compositeDisposable)
            }
        } else {
            progressBar.visibility = View.GONE
        }

        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        recyclerView.adapter = adapter

    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }
}