package com.hmomeni.canto.activities

import android.animation.Animator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import com.crashlytics.android.Crashlytics
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.rcl.AvatarsRclAdapter
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.vms.EditUserViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_edit_user.*
import timber.log.Timber
import java.util.regex.Pattern

class EditUserActivity : BaseActivity() {
    lateinit var viewModel: EditUserViewModel
    private val compositeDisposable = CompositeDisposable()

    private var isUserNameValid = false
    private var selectedAvatar: Int = -1
    private val userNamePattern = Pattern.compile("(^[a-zA-Z]+)([a-zA-Z0-9_]*)")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, ViewModelFactory(app()))[EditUserViewModel::class.java]
        setContentView(R.layout.activity_edit_user)

        userName.setText(viewModel.userSession.user?.username)
        viewModel.userSession.user?.avatar?.let {
            selectedAvatar = it.id
            GlideApp.with(userPhoto)
                    .load(it.link)
                    .rounded(dpToPx(10))
                    .into(userPhoto)
        }

        recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 3)
        recyclerView.adapter = AvatarsRclAdapter(viewModel.avatars).also {
            it.clickPublisher.subscribe {
                val avatar = viewModel.avatars[it]
                selectedAvatar = avatar.id
                GlideApp.with(userPhoto)
                        .load(avatar.link)
                        .rounded(dpToPx(10))
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
        userName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                displayUserNameStatus()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })
        confirmBtn.setOnClickListener {
            if (!isUserNameValid) {
                Toast.makeText(this, R.string.choose_valid_username, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedAvatar < 0) {
                Toast.makeText(this, R.string.avatar_is_mandatory, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val progressDialog = ProgressDialog(this)
            viewModel.updateUser(selectedAvatar, userName.text.toString())
                    .iomain()
                    .doOnSubscribe { progressDialog.show() }
                    .doAfterTerminate { progressDialog.dismiss() }
                    .subscribe({
                        Toast.makeText(this, R.string.user_info_updated_successfully, Toast.LENGTH_SHORT).show()
                        finish()
                    }, {
                        Timber.e(it)
                        Crashlytics.logException(it)
                        Toast.makeText(this, R.string.updating_user_failed, Toast.LENGTH_SHORT).show()
                    }).addTo(compositeDisposable)
        }
        loadAvatars()

        backBtn.setOnClickListener { finish() }
    }

    private fun loadAvatars() {
        viewModel.getAvatars().iomain()
                .subscribe({
                    recyclerView.adapter!!.notifyDataSetChanged()
                    if (it.isNotEmpty()) {
                        loadAvatars()
                    }
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

    private fun displayUserNameStatus() {
        if (userName.text.isEmpty()) {
            resultImage.visibility = View.GONE
            resultText.visibility = View.GONE
            isUserNameValid = false
            return
        }
        resultImage.visibility = View.VISIBLE
        resultText.visibility = View.VISIBLE

        isUserNameValid = userNamePattern.matcher(userName.text.toString()).matches()

        if (!isUserNameValid) {
            resultImage.setImageResource(R.drawable.ic_error)
            resultText.setText(R.string.choose_valid_username)
        } else {
            resultImage.setImageResource(R.drawable.ic_success)
            resultText.setText(R.string.valid_username)
        }
    }
}
