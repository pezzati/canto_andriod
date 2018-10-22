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
import com.hmomeni.canto.entities.Genre
import com.hmomeni.canto.utils.ViewModelFactory
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.iomain
import com.hmomeni.canto.utils.navigation.ListNavEvent
import com.hmomeni.canto.vms.MainViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_main.*
import timber.log.Timber

class MainFragment : Fragment() {
    private lateinit var viewModel: MainViewModel
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, ViewModelFactory(context!!.app()))[MainViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        val genres = mutableListOf<Genre>()
        viewModel.api.getBanners()
                .map { it.data }
                .iomain()
                .subscribe({
                    recyclerView.adapter = MainRclAdapter(it, genres).also {
                        it.clickPublisher.subscribe {
                            when (it.first) {
                                0 -> {
                                }
                                else -> {
                                    val pos = it.first - 1
                                    val genre = genres[pos]
                                    viewModel.navEvents.onNext(ListNavEvent("genre", genre.filesLink.replace(Regex("[^\\d]"), "").toInt()))
                                }
                            }
                        }.addTo(compositeDisposable)
                    }
                    viewModel.api.getGenres()
                            .map { it.data }
                            .iomain()
                            .subscribe({
                                it.forEach { genre ->
                                    val genreId = genre.filesLink.replace(Regex("[^\\d]"), "").toInt()
                                    viewModel.api
                                            .getGenrePosts(genreId)
                                            .map { it.data }
                                            .iomain()
                                            .subscribe({
                                                genre.posts = it
                                                genres.add(genre)
                                                recyclerView.adapter.notifyDataSetChanged()
                                            }, {
                                                Timber.e(it)
                                            })

                                }
                            }, {
                                Timber.e(it)
                            }).addTo(compositeDisposable)
                }, {
                    Timber.e(it)
                }).addTo(compositeDisposable)
    }

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }
}