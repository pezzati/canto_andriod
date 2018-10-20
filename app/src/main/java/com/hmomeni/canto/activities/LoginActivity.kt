package com.hmomeni.canto.activities

import android.animation.Animator
import android.app.ProgressDialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.MyAnimatorListener
import com.hmomeni.canto.utils.ViewModelFactory
import com.hmomeni.canto.utils.app
import com.hmomeni.canto.utils.iomain
import com.hmomeni.canto.vms.LoginViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_login.*
import timber.log.Timber

class LoginActivity : AppCompatActivity() {
    private lateinit var viewModel: LoginViewModel
    private val compositeDisposable = CompositeDisposable()

    private var step = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, ViewModelFactory(app()))[LoginViewModel::class.java]

        setContentView(R.layout.activity_login)

        phoneBtn.setOnClickListener {
            when (step) {
                0 -> goToPhoneInput()
                1 -> submitPhone()
                2 -> submitCode()
            }
        }
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

    private var phoneOriginalY = 0f
    private lateinit var phoneInputWrapperGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener
    private fun goToPhoneInput() {
        phoneOriginalY = phoneBtn.y
        phoneInputWrapperGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (phoneInputWrapper.measuredHeight > 0 && step == 0) {
                step = 1
                phoneInputWrapper.animate().alpha(1f)
                cantoWrapper.animate().scaleX(0.8f).scaleY(0.8f).translationYBy(-100f)
                googleBtn.animate().alpha(0f)
                continueWith.animate().alpha(0f)
                emailBtn.animate().alpha(0f)
                phoneBtn.text = getString(R.string.send)
                phoneBtn.animate().y(phoneInputWrapper.y + phoneInputWrapper.height + 50).setListener(object : MyAnimatorListener() {
                    override fun onAnimationEnd(animation: Animator?) {
                        googleBtn.visibility = View.GONE
                        emailBtn.visibility = View.GONE
                        continueWith.visibility = View.GONE
                    }
                })
                phoneInputWrapper.viewTreeObserver.removeOnGlobalLayoutListener(phoneInputWrapperGlobalLayoutListener)
            }
        }
        phoneInputWrapper.viewTreeObserver.addOnGlobalLayoutListener(phoneInputWrapperGlobalLayoutListener)
        phoneInputWrapper.visibility = View.VISIBLE
    }

    private lateinit var codeInputWrapperGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener
    private fun goToCodeInput() {
        codeInputWrapperGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (codeInputWrapper.measuredHeight > 0 && step == 1) {
                step = 2
                phoneInputWrapper.animate().alpha(0f)
                codeInputWrapper.animate().alpha(1f)
                phoneBtn.animate().y(codeInputWrapper.y + codeInputWrapper.height + 50).setListener(object : MyAnimatorListener() {
                    override fun onAnimationEnd(animation: Animator?) {
                        phoneInputWrapper.visibility = View.GONE
                    }
                })
                codeInputWrapper.viewTreeObserver.removeOnGlobalLayoutListener(codeInputWrapperGlobalLayoutListener)
            }
        }
        codeInputWrapper.viewTreeObserver.addOnGlobalLayoutListener(codeInputWrapperGlobalLayoutListener)
        codeInputWrapper.visibility = View.VISIBLE
    }

    private fun backToPhoneInput() {
        step = 1
        phoneInputWrapper.visibility = View.VISIBLE
        phoneInputWrapper.animate().alpha(1f)
        codeInputWrapper.animate().alpha(0f)
        phoneBtn.animate().y(phoneInputWrapper.y + phoneInputWrapper.height + 50).setListener(object : MyAnimatorListener() {
            override fun onAnimationEnd(animation: Animator?) {
                codeInputWrapper.visibility = View.GONE
            }
        })
    }

    private fun backToOriginFromPhone() {
        step = 0
        googleBtn.visibility = View.VISIBLE
        emailBtn.visibility = View.VISIBLE
        continueWith.visibility = View.VISIBLE
        phoneInputWrapper.animate().alpha(0f)
        cantoWrapper.animate().scaleX(1f).scaleY(1f).translationY(0f)
        googleBtn.animate().alpha(1f)
        continueWith.animate().alpha(1f)
        emailBtn.animate().alpha(1f)
        phoneBtn.text = getString(R.string.send)
        phoneBtn.animate().y(phoneOriginalY).setListener(object : MyAnimatorListener() {
            override fun onAnimationEnd(animation: Animator?) {
                phoneInputWrapper.visibility = View.GONE
            }
        })
    }

    private fun submitPhone() {
        val progressDialog = ProgressDialog(this).apply {
            isIndeterminate = true
        }

        viewModel.signUpPhone(phoneInput.text.toString())
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

        viewModel.verifyPhone(codeInput.text.toString())
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
