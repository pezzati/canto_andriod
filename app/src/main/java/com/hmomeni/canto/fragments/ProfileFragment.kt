package com.hmomeni.canto.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import com.crashlytics.android.Crashlytics
import com.hmomeni.canto.R
import com.hmomeni.canto.activities.EditUserActivity
import com.hmomeni.canto.activities.InfoActivity
import com.hmomeni.canto.activities.ShopActivity
import com.hmomeni.canto.adapters.rcl.ProjectsRclAdapter
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.utils.navigation.ProjectEvent
import com.hmomeni.canto.vms.ProfileViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_profile.*
import timber.log.Timber

class ProfileFragment : Fragment(), View.OnClickListener {

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
                .subscribe({ l ->
                    if (l.isEmpty()) {
                        showNoPost()
                    }
                    recyclerView.adapter = ProjectsRclAdapter(l).also {
                        it.clickPublisher.subscribe { pos ->
                            viewModel.navEvents.onNext(ProjectEvent(l[pos].projectId))
                        }
                    }
                }, {
                    showNoPost()
                    Crashlytics.logException(it)
                }).addTo(compositeDisposable)

        recyclerView.setPadding(0, getScreenDimensions(context!!).height / 4, 0, 0)

        btnSettings.setOnClickListener(this)
        btnShop.setOnClickListener(this)
        btnInfo.setOnClickListener(this)
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
                    btnShop.visibility = View.VISIBLE
                    btnInfo.visibility = View.VISIBLE
                    userName.text = it.username
                    it.avatar?.let {
                        GlideApp.with(userPhoto)
                                .load(it.link)
                                .placeholder(R.drawable.ic_user_place_holder)
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

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnSettings -> startActivity(Intent(context!!, EditUserActivity::class.java))
            R.id.btnShop -> startActivity(Intent(context!!, ShopActivity::class.java))
            R.id.btnInfo -> startActivity(Intent(context!!, InfoActivity::class.java))
        }
    }

    private fun showNoPost() {
        noPostImage.visibility = View.VISIBLE
        noPostText.visibility = View.VISIBLE
        GlideApp.with(noPostImage)
                .load(R.drawable.not_post_yet)
                .into(noPostImage)
    }
}