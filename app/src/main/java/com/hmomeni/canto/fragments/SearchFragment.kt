package com.hmomeni.canto.fragments

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.rcl.ListPostsRclAdapter
import com.hmomeni.canto.entities.Post
import com.hmomeni.canto.utils.ViewModelFactory
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.iomain
import com.hmomeni.canto.vms.SearchViewModel
import com.jakewharton.rxbinding3.widget.afterTextChangeEvents
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_search.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SearchFragment : Fragment() {
    private lateinit var viewModel: SearchViewModel
    private val compositeDisposable = CompositeDisposable()
    private val posts: MutableList<Post> = mutableListOf()
    private val adapter = ListPostsRclAdapter(posts, R.layout.rcl_item_list_post)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, ViewModelFactory(context!!.app()))[SearchViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    private var searchDisposable: Disposable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        searchInput.afterTextChangeEvents()
                .filter { !it.editable.isNullOrEmpty() }
                .debounce(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    searchDisposable?.let {
                        it.dispose()
                        compositeDisposable.remove(it)
                    }
                    searchDisposable = viewModel.api.searchInGenres(it.editable.toString())
                            .map { it.data }
                            .iomain()
                            .doOnSubscribe {
                                progressBar.visibility = View.VISIBLE
                                searchImage.visibility = View.GONE
                            }
                            .doAfterTerminate {
                                progressBar.visibility = View.GONE
                                searchImage.visibility = View.VISIBLE
                            }
                            .subscribe({
                                posts.clear()
                                posts.addAll(it)
                                adapter.notifyDataSetChanged()
                            }, {
                                Timber.e(it)
                            })
                    compositeDisposable.add(searchDisposable!!)
                }.addTo(compositeDisposable)


        recyclerView.layoutManager = LinearLayoutManager(context!!)
        recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }
}