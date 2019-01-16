package com.hmomeni.canto.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.hmomeni.canto.R
import com.hmomeni.canto.activities.EditUserActivity
import com.hmomeni.canto.adapters.rcl.ProjectsRclAdapter
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.utils.navigation.ProjectEvent
import com.hmomeni.canto.vms.ProfileViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_profile.*
import timber.log.Timber

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
        recyclerView.layoutManager = GridLayoutManager(context!!, 2)

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

        recyclerView.setPadding(0, getScreenDimensions(context!!).height / 4, 0, 0)

        btnSettings.setOnClickListener {
            startActivity(Intent(context!!, EditUserActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel
                .getUser()
                .iomain()
                .doAfterTerminate { progressBar.visibility = View.GONE }
                .subscribe({
                    userName.visibility = View.VISIBLE
                    userPhoto.visibility = View.VISIBLE
                    btnSettings.visibility = View.VISIBLE
                    userName.text = it.username
                    it.avatar?.let {
                        GlideApp.with(userPhoto)
                                .load(it.link)
                                .rounded(dpToPx(10))
                                .into(userPhoto)
                    }
                }, {
                    Timber.e(it)
                }).addTo(compositeDisposable)
    }
    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }
}