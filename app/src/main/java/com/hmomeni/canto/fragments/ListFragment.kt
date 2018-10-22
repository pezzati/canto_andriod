package com.hmomeni.canto.fragments

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.rcl.ListPostsRclAdapter
import com.hmomeni.canto.utils.ViewModelFactory
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.iomain
import com.hmomeni.canto.vms.ListViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_list.*
import timber.log.Timber

class ListFragment : Fragment() {

    companion object {
        fun newInstance(type: String, objectId: Int): ListFragment {
            val bundle = Bundle()
            bundle.putString("type", type)
            bundle.putInt("object_id", objectId)
            return ListFragment().apply {
                arguments = bundle
            }
        }

        fun getBundle(type: String, objectId: Int): Bundle {
            return Bundle().apply {
                putString("type", type)
                putInt("object_id", objectId)
            }
        }
    }

    private lateinit var viewModel: ListViewModel
    private val compositeDisposable = CompositeDisposable()

    private var listAdapter: ListPostsRclAdapter? = null
    private var gridAdapter: ListPostsRclAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, ViewModelFactory(context!!.app()))[ListViewModel::class.java]

        arguments?.let {
            viewModel.type = it.getString("type")
            viewModel.objectId = it.getInt("object_id")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_list, container, false)

    private var isList = true
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        listAdapter = ListPostsRclAdapter(viewModel.posts, R.layout.rcl_item_list_post)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = listAdapter
        viewModel
                .loadPosts()
                .iomain()
                .subscribe({
                    recyclerView.adapter.notifyItemRangeInserted(0, viewModel.posts.size)
                }, {
                    Timber.e(it)
                })
                .addTo(compositeDisposable)

        toggleListMode.setOnClickListener {
            when (isList) {
                false -> {
                    if (listAdapter == null) {
                        listAdapter = ListPostsRclAdapter(viewModel.posts, R.layout.rcl_item_list_post)
                    }
                    recyclerView.swapAdapter(listAdapter, false)
                    recyclerView.layoutManager = LinearLayoutManager(context!!)
                    isList = true
                }
                true -> {
                    if (gridAdapter == null) {
                        gridAdapter = ListPostsRclAdapter(viewModel.posts, R.layout.rcl_item_grid_post)
                    }
                    recyclerView.swapAdapter(gridAdapter, false)
                    recyclerView.layoutManager = GridLayoutManager(context!!, 2)
                    isList = false
                }
            }
        }
    }
}