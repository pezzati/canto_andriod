package com.hmomeni.canto.fragments

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.ViewModelFactory
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.vms.ListViewModel

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
    }

    private lateinit var viewModel: ListViewModel

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    }
}