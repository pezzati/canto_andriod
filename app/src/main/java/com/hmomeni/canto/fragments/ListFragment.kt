package com.hmomeni.canto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crashlytics.android.Crashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.rcl.ListPostsRclAdapter
import com.hmomeni.canto.utils.iomain
import com.hmomeni.canto.utils.navigation.PostNavEvent
import com.hmomeni.canto.vms.ListViewModel
import com.hmomeni.canto.vms.injector
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_list.*
import timber.log.Timber

class ListFragment : BaseFragment() {

    companion object {
        fun getBundle(type: String, objectId: Int, title: String, urlPath: String): Bundle {
            return Bundle().apply {
                putString("type", type)
                putString("title", title)
                putInt("object_id", objectId)
                putString("url_path", urlPath)
            }
        }
    }

    private lateinit var title: String
    private lateinit var viewModel: ListViewModel
    private val compositeDisposable = CompositeDisposable()

    private var listAdapter: ListPostsRclAdapter? = null
    private var gridAdapter: ListPostsRclAdapter? = null

    private val args by navArgs<ListFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, injector.listViewModelFactory())[ListViewModel::class.java]

        title = args.title
        viewModel.type = args.type
        viewModel.urlPath = args.urlPath
        FirebaseAnalytics.getInstance(context!!)
                .logEvent("List", Bundle().apply {
                    putString("url", viewModel.urlPath)
                    putString("type", viewModel.type)
                    putString("title", title)
                })
        listAdapter = ListPostsRclAdapter(viewModel.posts, R.layout.rcl_item_list_post)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_list, container, false)

    private var isList = true
    private var linearLayoutManager: LinearLayoutManager? = null
    private var gridLayoutManager: GridLayoutManager? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pageTitle.text = title
        backBtn.setOnClickListener {
            findNavController().navigateUp()
        }

        listAdapter?.let {
            it.clickPublisher.subscribe {
                viewModel.navEvents.onNext(PostNavEvent(viewModel.posts[it]))
            }.addTo(compositeDisposable)
        }

        linearLayoutManager = LinearLayoutManager(context)

        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = listAdapter

        applyEndlessScroll()

        viewModel
                .loadPosts()
                .iomain()
                .doAfterTerminate {
                    progressBar?.visibility = View.GONE
                }
                .subscribe({
                    recyclerView.adapter!!.notifyItemRangeInserted(0, viewModel.posts.size)
                }, {
                    Timber.e(it)
                    Crashlytics.logException(it)
                })
                .addTo(compositeDisposable)

        toggleListMode.setOnClickListener {
            when (isList) {
                false -> {
                    toggleListMode.setImageResource(R.drawable.ic_list_view)
                    if (listAdapter == null) {
                        listAdapter = ListPostsRclAdapter(viewModel.posts, R.layout.rcl_item_list_post)
                    }
                    if (linearLayoutManager == null) {
                        linearLayoutManager = LinearLayoutManager(context)
                    }
                    recyclerView.adapter = listAdapter
                    recyclerView.layoutManager = linearLayoutManager
                    isList = true
                }
                true -> {
                    toggleListMode.setImageResource(R.drawable.ic_list_card_view)
                    if (gridAdapter == null) {
                        gridAdapter = ListPostsRclAdapter(viewModel.posts, R.layout.rcl_item_grid_post)
                    }

                    if (gridLayoutManager == null) {
                        gridLayoutManager = GridLayoutManager(context!!, 2)
                    }

                    recyclerView.adapter = gridAdapter
                    recyclerView.layoutManager = gridLayoutManager
                    isList = false
                }
            }
        }
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }

    private fun applyEndlessScroll() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var endCalled = false
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (isList) {
                    val visibleItemCount = linearLayoutManager!!.childCount
                    val totalItemCount = linearLayoutManager!!.itemCount
                    val pastVisibleItems = linearLayoutManager!!.findFirstVisibleItemPosition()
                    endCalled = if (pastVisibleItems + visibleItemCount >= totalItemCount && !endCalled) {
                        viewModel
                                .loadNextPage()
                                ?.let {
                                    it.iomain()
                                            .subscribe({
                                                listAdapter?.notifyDataSetChanged()
                                            }, {
                                                Timber.e(it)
                                            }).addTo(compositeDisposable)
                                }
                        true
                    } else {
                        false
                    }
                } else {
                    val visibleItemCount = gridLayoutManager!!.childCount
                    val totalItemCount = gridLayoutManager!!.itemCount
                    val pastVisibleItems = gridLayoutManager!!.findFirstVisibleItemPosition()
                    endCalled = if (pastVisibleItems + visibleItemCount >= totalItemCount && !endCalled) {
                        viewModel
                                .loadNextPage()
                                ?.let {
                                    it.iomain()
                                            .subscribe({
                                                gridAdapter?.notifyDataSetChanged()
                                            }, {
                                                Timber.e(it)
                                            }).addTo(compositeDisposable)
                                }
                        true
                    } else {
                        false
                    }
                }

            }
        })
    }
}