package com.hmomeni.canto.fragments

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    private lateinit var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView.layoutManager = GridLayoutManager(context!!, 2)

        viewModel.projectDao
                .fetchCompleteProjects()
                .iomain()
                .map {
                    val list = it.toMutableList()
                    list.addAll(it)
                    list.addAll(it)
                    list.addAll(it)
                    list.addAll(it)
                    return@map list
                }
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
        userPhoto.setOnClickListener(this)
        userName.setOnClickListener(this)

        var xAnimator: ObjectAnimator? = null
        var yAnimator: ObjectAnimator? = null
        var scaleAnimator: ValueAnimator? = null


        var scrollOffset = 0
        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (btnInfo.x != 0f && btnInfo.y != 0f) {
                recyclerView.setPadding(0, guideline4.y.toInt(), 0, 0)
                scrollOffset = guideline4.y.toInt()

                xAnimator = ObjectAnimator.ofFloat(userPhoto, View.X, userPhoto.x, btnInfo.x - dpToPx(48 + 16))
                yAnimator = ObjectAnimator.ofFloat(userPhoto, View.Y, userPhoto.y, btnInfo.y - dpToPx(24 + 8))

                xAnimator!!.duration = 10000
                yAnimator!!.duration = 10000

                scaleAnimator = ValueAnimator.ofFloat(1f, dpToPx(24).toFloat() / userPhoto.height.toFloat()).apply {
                    duration = 10000
                    addUpdateListener {
                        userPhoto.scaleX = it.animatedValue as Float
                        userPhoto.scaleY = it.animatedValue as Float
                        userName.alpha = 1f - it.animatedFraction
                        userName.scaleX = 1f - it.animatedFraction
                        userName.scaleY = 1f - it.animatedFraction
                    }
                }
                btnInfo.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
            }

        }
        btnInfo.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)



        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val offset = recyclerView.computeVerticalScrollOffset()
                Timber.d("offset %d", offset)
                when {
                    offset == 0 -> {
                        xAnimator?.currentPlayTime = 0
                        yAnimator?.currentPlayTime = 0
                        scaleAnimator?.currentPlayTime = 0
                    }
                    offset < scrollOffset -> {
                        xAnimator?.currentPlayTime = (offset.toFloat() / scrollOffset.toFloat() * 10000).toLong()
                        yAnimator?.currentPlayTime = (offset.toFloat() / scrollOffset.toFloat() * 10000).toLong()
                        scaleAnimator?.currentPlayTime = (offset.toFloat() / scrollOffset.toFloat() * 10000).toLong()
                    }
                    offset >= scrollOffset -> {
                        xAnimator?.currentPlayTime = 10000
                        yAnimator?.currentPlayTime = 10000
                        scaleAnimator?.currentPlayTime = 10000
                    }
                }
            }
        })
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
            R.id.userName, R.id.userPhoto, R.id.btnSettings -> startActivity(Intent(context!!, EditUserActivity::class.java))
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