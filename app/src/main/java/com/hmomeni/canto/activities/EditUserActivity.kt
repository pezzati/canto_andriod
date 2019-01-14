package com.hmomeni.canto.activities

import android.animation.Animator
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.view.View
import com.crashlytics.android.Crashlytics
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.rcl.AvatarsRclAdapter
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.vms.EditUserViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_edit_user.*
import timber.log.Timber

class EditUserActivity : BaseActivity() {
    lateinit var viewModel: EditUserViewModel
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, ViewModelFactory(app()))[EditUserViewModel::class.java]
        setContentView(R.layout.activity_edit_user)

        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = AvatarsRclAdapter(viewModel.avatars).also {
            it.clickPublisher.subscribe {
                GlideApp.with(userPhoto)
                        .load(viewModel.avatars[it].link)
                        .rounded(10)
                        .into(userPhoto)
                hideAvatars()
            }.addTo(compositeDisposable)
        }

        btnEditAvatar.setOnClickListener {
            if (recyclerView.visibility == View.GONE) {
                showAvatars()
            } else {
                hideAvatars()
            }
        }

        viewModel.getAvatars().iomain()
                .subscribe({
                    recyclerView.adapter!!.notifyDataSetChanged()
                }, {
                    Timber.e(it)
                    Crashlytics.logException(it)
                }).addTo(compositeDisposable)
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (recyclerView.visibility == View.VISIBLE) {
            hideAvatars()
        } else {
            super.onBackPressed()
        }
    }

    private fun showAvatars() {
        recyclerView.visibility = View.VISIBLE
        recyclerView.animate().alpha(1f).scaleY(1f).scaleX(1f).setListener(null)
    }

    private fun hideAvatars() {
        recyclerView.animate().alpha(0f).scaleY(2f).scaleX(2f).setListener(object : MyAnimatorListener() {
            override fun onAnimationEnd(animation: Animator?) {
                recyclerView.visibility = View.GONE
            }
        })
    }
}
