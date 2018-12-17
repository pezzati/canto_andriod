package com.hmomeni.canto.activities

import android.animation.Animator
import android.app.ProgressDialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.vms.LoginViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_login.*
import timber.log.Timber

class LoginActivity : AppCompatActivity(), View.OnClickListener {


    private lateinit var viewModel: LoginViewModel
    private val compositeDisposable = CompositeDisposable()

    private var step = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProviders.of(this, ViewModelFactory(app()))[LoginViewModel::class.java]

        setContentView(R.layout.activity_login)

        Glide.with(this)
                .load(R.drawable.splash_screen)
                .into(splashBackground)

        phoneBtn.setOnClickListener(this)
        emailBtn.setOnClickListener(this)
        loginBtn.setOnClickListener(this)
        verifyBtn.setOnClickListener(this)

        cantoWrapper.translationY += getScreenDimensions(this).height / 6

        viewModel.handshake()
                .iomain()
                .subscribe({ p ->
                    when (p.first) {
                        0 -> prepareLogin()
                        3 -> {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        2 -> {
                            prepareLogin()
                            CantoDialog(this, getString(R.string.update_required), getString(R.string.update_rationale), autoDismiss = false, positiveListener = {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.second)))
                            }, showNegativeButton = true, negativeListener = { d -> d.dismiss() }).show()
                        }
                        1 -> CantoDialog(this, getString(R.string.update_required), getString(R.string.update_rationale), autoDismiss = false, positiveListener = {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.second)))
                        }).apply {
                            setCanceledOnTouchOutside(false)
                        }.show()
                    }
                }, {
                    Timber.e(it)
                }).addTo(compositeDisposable)
    }

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }

    override fun onBackPressed() {
        when (step) {
            0 -> super.onBackPressed()
            1 -> backToOriginFromPhone()
            2 -> backToPhoneInput()
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.phoneBtn -> goToPhoneInput()
            R.id.emailBtn -> goToPhoneInput(true)
            R.id.loginBtn -> submitPhone()
            R.id.verifyBtn -> submitCode()
        }
    }

    private fun prepareLogin() {
        val height = getScreenDimensions(this).height
        cantoWrapper.animate().translationYBy(-height / 6f)
        buttonsWrapper.postDelayed({
            buttonsWrapper.visibility = View.VISIBLE
            buttonsWrapper.animate().alpha(1f)
        }, 300)
    }

    private fun goToPhoneInput(email: Boolean = false) {
        if (email) {
            viewModel.signupMode = LoginViewModel.SignupMode.EMAIL
            loginTitle.setText(R.string.login_by_email)
            phoneInput.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            phoneInput.setHint(R.string.email)
        } else {
            viewModel.signupMode = LoginViewModel.SignupMode.PHONE
            loginTitle.setText(R.string.login_by_phone_number)
            phoneInput.inputType = InputType.TYPE_CLASS_PHONE
            phoneInput.setHint(R.string.phone_number)
        }
        step = 1
        phoneInputWrapper.animate().alpha(1f)
        cantoWrapper.animate().scaleX(0.8f).scaleY(0.8f).translationYBy(-100f)
        buttonsWrapper.animate().alpha(0f).setListener(object : MyAnimatorListener() {
            override fun onAnimationEnd(animation: Animator?) {
                buttonsWrapper.visibility = View.GONE
            }
        })

        phoneInputWrapper.visibility = View.VISIBLE
    }

    private fun goToCodeInput() {
        step = 2
        codeInputWrapper.visibility = View.VISIBLE
        phoneInputWrapper.animate().alpha(0f)
        codeInputWrapper.animate().alpha(1f).setListener(object : MyAnimatorListener() {
            override fun onAnimationEnd(animation: Animator?) {
                phoneInputWrapper.visibility = View.GONE
            }
        })
    }

    private fun backToPhoneInput() {
        step = 1
        phoneInputWrapper.visibility = View.VISIBLE
        phoneInputWrapper.animate().alpha(1f)
        codeInputWrapper.animate().alpha(0f).setListener(object : MyAnimatorListener() {
            override fun onAnimationEnd(animation: Animator?) {
                codeInputWrapper.visibility = View.GONE
            }
        })
    }

    private fun backToOriginFromPhone() {
        step = 0
        buttonsWrapper.visibility = View.VISIBLE
        phoneInputWrapper.animate().alpha(0f)
        cantoWrapper.animate().scaleX(1f).scaleY(1f).translationY(0f)
        buttonsWrapper.animate().alpha(1f).setListener(object : MyAnimatorListener() {
            override fun onAnimationEnd(animation: Animator?) {
                phoneInputWrapper.visibility = View.GONE
            }
        })

    }

    private fun submitPhone() {
        val progressDialog = ProgressDialog(this).apply {
            isIndeterminate = true
        }

        viewModel.signUp(phoneInput.text.toString())
                .iomain()
                .doOnSubscribe { progressDialog.show() }
                .doAfterTerminate { progressDialog.dismiss() }
                .subscribe({
                    goToCodeInput()
                }, {
                    Toast.makeText(this, R.string.failed_requstin_verification, Toast.LENGTH_SHORT).show()
                    Timber.e(it)
                }).addTo(compositeDisposable)
    }

    private fun submitCode() {
        val progressDialog = ProgressDialog(this).apply {
            isIndeterminate = true
        }

        viewModel.verify(codeInput.text.toString())
                .iomain()
                .doOnSubscribe { progressDialog.show() }
                .doAfterTerminate { progressDialog.dismiss() }
                .subscribe({
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }, {
                    Toast.makeText(this, R.string.failed_requstin_verification, Toast.LENGTH_SHORT).show()
                    Timber.e(it)
                }).addTo(compositeDisposable)
    }


}
