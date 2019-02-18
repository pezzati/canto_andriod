package com.hmomeni.canto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.rcl.ListPostsRclAdapter
import com.hmomeni.canto.entities.UserAction
import com.hmomeni.canto.utils.addUserAction
import com.hmomeni.canto.utils.iomain
import com.hmomeni.canto.utils.navigation.PostNavEvent
import com.hmomeni.canto.vms.SearchViewModel
import com.hmomeni.canto.vms.injector
import com.jakewharton.rxbinding3.widget.afterTextChangeEvents
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_search.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SearchFragment : BaseFragment() {
    private lateinit var viewModel: SearchViewModel
    private val compositeDisposable = CompositeDisposable()
    private lateinit var adapter: ListPostsRclAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, injector.searchViewModelFactory())[SearchViewModel::class.java]
        adapter = ListPostsRclAdapter(viewModel.result, R.layout.rcl_item_list_post)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    private var searchDisposable: Disposable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        searchInput.afterTextChangeEvents()
                .debounce(1000, TimeUnit.MILLISECONDS)
                .filter { !it.editable.isNullOrBlank() }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    searchDisposable?.let {
                        it.dispose()
                        compositeDisposable.remove(it)
                    }
                    if (it.editable.isNullOrBlank()) {
                        return@subscribe
                    }
                    FirebaseAnalytics.getInstance(context!!)
                            .logEvent(FirebaseAnalytics.Event.SEARCH, Bundle().apply {
                                putString(FirebaseAnalytics.Param.SEARCH_TERM, it.editable.toString())
                            })
                    addUserAction(UserAction("Searched", detail = it.editable.toString()))
                    searchDisposable = viewModel.search(it.editable.toString())
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
                            }, {
                                Timber.e(it)
                            }).addTo(compositeDisposable)
                }.addTo(compositeDisposable)


        recyclerView.layoutManager = LinearLayoutManager(context!!)
        recyclerView.adapter = adapter

        viewModel
                .postsPublisher
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    adapter.notifyDataSetChanged()
                }.addTo(compositeDisposable)

        adapter.clickPublisher.subscribe {
            viewModel.navEvents.onNext(PostNavEvent(adapter.posts[it]))
        }.addTo(compositeDisposable)
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }
}