package com.hmomeni.canto.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.EmptyResultSetException
import com.crashlytics.android.Crashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.rcl.ProjectsRclAdapter
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.vms.ProfileViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_profile.*
import timber.log.Timber
import java.util.*
import kotlin.math.abs

class ProfileFragment : BaseFragment(), View.OnClickListener {

    private lateinit var viewModel: ProfileViewModel
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, ViewModelFactory(context!!.app()))[ProfileViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    private lateinit var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener

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
                            findNavController().navigate(ProfileFragmentDirections.actionProfileFragmentToVideoPlayActivity(l[pos].projectId.toInt()))
                        }
                    }
                }, {
                    showNoPost()
                    if (it !is EmptyResultSetException) {
                        Crashlytics.logException(it)
                    }
                }).addTo(compositeDisposable)

        recyclerView.setPadding(0, getScreenDimensions(context!!).height / 4, 0, 0)

        btnSettings.setOnClickListener(this)
        btnShop.setOnClickListener(this)
        btnInfo.setOnClickListener(this)
        userPhoto.setOnClickListener(this)
        userName.setOnClickListener(this)


        val maxScroll = dpToPx(160)
        var scrolled = 0
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val offset = recyclerView.computeVerticalScrollOffset()
                Timber.d("offset %d", offset)
                if (offset == 0) {
                    scrolled = 0
                    header.translationY = 0f
                } else {
                    scrolled = -offset
                    if (abs(scrolled) < maxScroll) {
                        header.translationY = scrolled.toFloat()
                    } else {
                        header.translationY = (-maxScroll).toFloat()
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewModel
                .getUser()
                .filter { it.id >= 0 }
                .iomain()
//                .doAfterTerminate { progressBar.visibility = View.GONE }
                .subscribe({
                    FirebaseAnalytics.getInstance(context!!).setUserId(it.id.toString())
                    Crashlytics.setUserIdentifier(it.id.toString())

                    userGroup.visible()
//                    progressBar.gone()

                    userName.text = it.username
                    if (it.premiumDays > 0) {
                        currentBalance.text = getString(R.string.x_days, it.premiumDays)
                    } else {
                        currentBalance.text = java.text.NumberFormat.getInstance(Locale.ENGLISH).format(it.coins)
                    }
                    it.avatar?.let {
                        GlideApp.with(userPhoto)
                                .load(it.link)
                                .placeholder(R.drawable.ic_user_place_holder)
                                .rounded(dpToPx(10))
                                .into(userPhoto)
                    }
                }, {
                    Timber.e(it, "failed loading user")
                }).addTo(compositeDisposable)
    }

    override fun onDestroyView() {
        compositeDisposable.clear()
        super.onDestroyView()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.userName, R.id.userPhoto, R.id.btnSettings -> findNavController().navigate(ProfileFragmentDirections.actionProfileFragmentToEditUserFragment())
            R.id.btnShop -> findNavController().navigate(ProfileFragmentDirections.actionProfileFragmentToShopActivity())
            R.id.btnInfo -> findNavController().navigate(ProfileFragmentDirections.actionProfileFragmentToInfoFragment())
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