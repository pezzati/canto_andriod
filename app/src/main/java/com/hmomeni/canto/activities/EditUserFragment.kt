package com.hmomeni.canto.activities

import android.animation.Animator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.crashlytics.android.Crashlytics
import com.hmomeni.canto.R
import com.hmomeni.canto.adapters.rcl.AvatarsRclAdapter
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.vms.EditUserViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.fragment_edit_user.*
import timber.log.Timber
import java.util.regex.Pattern


class EditUserFragment : Fragment() {
    lateinit var viewModel: EditUserViewModel
    private val compositeDisposable = CompositeDisposable()

    private var isUserNameValid = false
    private var selectedAvatar: Int = -1
    private val userNamePattern = Pattern.compile("(^[a-zA-Z]+)([a-zA-Z0-9_]*)")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, ViewModelFactory(context!!.app()))[EditUserViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(com.hmomeni.canto.R.layout.fragment_edit_user, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        userName.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
                if (keyCode == KeyEvent.KEYCODE_BACK && recyclerView.visibility == View.VISIBLE) {
                    hideAvatars()
                    return true
                }
                return false
            }
        })

        userName.setText(viewModel.userSession.user?.username)
        viewModel.userSession.user?.avatar?.let {
            selectedAvatar = it.id
            GlideApp.with(userPhoto)
                    .load(it.link)
                    .rounded(dpToPx(10))
                    .into(userPhoto)
        }

        recyclerView.layoutManager = GridLayoutManager(context!!, 3)
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
                Toast.makeText(context!!, R.string.choose_valid_username, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedAvatar < 0) {
                Toast.makeText(context!!, R.string.avatar_is_mandatory, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val progressDialog = ProgressDialog(context!!)
            viewModel.updateUser(selectedAvatar, userName.text.toString())
                    .iomain()
                    .doOnSubscribe { progressDialog.show() }
                    .doAfterTerminate { progressDialog.dismiss() }
                    .subscribe({
                        Toast.makeText(context!!, R.string.user_info_updated_successfully, Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }, {
                        Timber.e(it)
                        Crashlytics.logException(it)
                        Toast.makeText(context!!, R.string.updating_user_failed, Toast.LENGTH_SHORT).show()
                    }).addTo(compositeDisposable)
        }
        loadAvatars()

        backBtn.setOnClickListener { findNavController().popBackStack() }
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
