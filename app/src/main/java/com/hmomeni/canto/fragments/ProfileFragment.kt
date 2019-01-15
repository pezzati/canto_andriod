package com.hmomeni.canto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.rcl.ProjectsRclAdapter
import com.hmomeni.canto.utils.ViewModelFactory
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.iomain
import com.hmomeni.canto.utils.navigation.ProjectEvent
import com.hmomeni.canto.vms.ProfileViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_profile.*

class ProfileFragment : androidx.fragment.app.Fragment() {

    private lateinit var viewModel: ProfileViewModel
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, ViewModelFactory(context!!.app()))[ProfileViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(context!!, 2)

        viewModel.projectDao
                .fetchCompleteProjects()
                .iomain()
                .subscribe { l ->
                    recyclerView.adapter = ProjectsRclAdapter(l).also {
                        it.clickPublisher.subscribe { pos ->
                            viewModel.navEvents.onNext(ProjectEvent(l[pos].projectId))
                        }
                    }
                }.addTo(compositeDisposable)
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }
}