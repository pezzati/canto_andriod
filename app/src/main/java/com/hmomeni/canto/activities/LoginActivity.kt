package com.hmomeni.canto.activities

import android.animation.Animator
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.vms.LoginViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_login.*
import timber.log.Timber
import java.util.regex.Pattern


class LoginActivity : BaseActivity(), View.OnClickListener {


    private lateinit var viewModel: LoginViewModel
    private val compositeDisposable = CompositeDisposable()

    private var step = 0
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    private val GOOGLE_SIGNIN_REQ_CODE = 4543

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        viewModel = ViewModelProviders.of(this, ViewModelFactory(app()))[LoginViewModel::class.java]

        setContentView(R.layout.activity_login)

        phoneBtn.setOnClickListener(this)
        emailBtn.setOnClickListener(this)
        loginBtn.setOnClickListener(this)
        googleBtn.setOnClickListener(this)
        verifyBtn.setOnClickListener(this)
        wrongPhoneBtn.setOnClickListener(this)
        noCodeBtn.setOnClickListener(this)


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
                            PaymentDialog(this,
                                    getString(R.string.update_required),
                                    getString(R.string.update_suggest_rationale),
                                    imageResId = R.drawable.update,
                                    showPositiveButton = true,
                                    showNegativeButton = true,
                                    positiveButtonText = getString(R.string.update),
                                    negativeButtonText = getString(R.string.ask_later),
                                    positiveListener = {
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.second)))
                                    }).show()
                        }
                        1 -> PaymentDialog(this,
                                getString(R.string.update_required),
                                getString(R.string.update_suggest_rationale),
                                imageResId = R.drawable.update,
                                showPositiveButton = true,
                                showNegativeButton = true,
                                positiveButtonText = getString(R.string.update),
                                negativeButtonText = getString(R.string.ask_later),
                                positiveListener = {
                                    if (!p.second.isNullOrEmpty()) {
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.second)))
                                    }
                                }).apply { setCanceledOnTouchOutside(false) }.show()
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
            R.id.wrongPhoneBtn -> backToPhoneInput()
            R.id.noCodeBtn -> submitPhone()
            R.id.googleBtn -> signInByGoogle()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_SIGNIN_REQ_CODE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.googleSignIn(account!!.id!!)
                        .iomain()
                        .subscribe({
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }, {
                            Toast.makeText(this, R.string.failed_requstin_verification, Toast.LENGTH_SHORT).show()
                            Timber.e(it)
                        }).addTo(compositeDisposable)
            } catch (e: ApiException) {

            }
        }
    }

    private fun prepareLogin() {
        if (!viewModel.isFFMpegAvailable()) {
            progressBar.visibility = View.VISIBLE
            viewModel.downloadFFMpeg()
                    .iomain()
                    .subscribe({
                        progressBar.progress = it
                    }, {
                        progressBar.visibility = View.GONE
                    }, {
                        progressBar.visibility = View.GONE
                        prepareLogin()
                    }).addTo(compositeDisposable)
        } else {
            val height = getScreenDimensions(this).height
            cantoWrapper.animate().translationYBy(-height / 6f)
            buttonsWrapper.postDelayed({
                buttonsWrapper.visibility = View.VISIBLE
                buttonsWrapper.animate().alpha(1f)
            }, 300)
        }
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
        cantoWrapper.animate().alpha(0f).setListener(object : MyAnimatorListener() {
            override fun onAnimationEnd(animation: Animator?) {
                cantoWrapper.visibility = View.GONE
            }

        })
        buttonsWrapper.animate().alpha(0f).setListener(object : MyAnimatorListener() {
            override fun onAnimationEnd(animation: Animator?) {
                buttonsWrapper.visibility = View.GONE
            }
        })

        phoneInputWrapper.visibility = View.VISIBLE
    }

    private fun goToCodeInput() {
        if (step == 2) return

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
        timerCanceled = true
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
        cantoWrapper.visibility = View.VISIBLE
        cantoWrapper.animate().alpha(1f).setListener(null)
        buttonsWrapper.animate().alpha(1f).setListener(object : MyAnimatorListener() {
            override fun onAnimationEnd(animation: Animator?) {
                phoneInputWrapper.visibility = View.GONE
            }
        })

    }

    private fun submitPhone() {

        if (!verifyInput(phoneInput.text.toString(), viewModel.signupMode)) {
            showErrorMessage(viewModel.signupMode)
            return
        }

        val progressDialog = ProgressDialog(this).apply {
            isIndeterminate = true
        }

        viewModel.signUp(phoneInput.text.toString())
                .iomain()
                .doOnSubscribe { progressDialog.show() }
                .doAfterTerminate { progressDialog.dismiss() }
                .subscribe({
                    phoneNumberView.text = phoneInput.text
                    noCodeBtn.isClickable = false
                    goToCodeInput()
                    timerCanceled = false
                    startCountDownTimer(45)
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

    private var timerCanceled = false
    private fun startCountDownTimer(start: Int = 45) {
        codeTimer.text = "%02d:%02d".format(start / 60, start % 60)
        if (start > 0 && !timerCanceled) {
            codeTimer.postDelayed({
                startCountDownTimer(start - 1)
            }, 1000)
        } else {
            noCodeBtn.isClickable = true
        }
    }

    private fun verifyInput(input: String, mode: LoginViewModel.SignupMode): Boolean {
        return when (mode) {
            LoginViewModel.SignupMode.EMAIL -> Patterns.EMAIL_ADDRESS.matcher(input).matches()
            LoginViewModel.SignupMode.PHONE -> Pattern.compile("^09(\\d){9}").matcher(input).matches()
        }
    }

    private fun showErrorMessage(mode: LoginViewModel.SignupMode) {
        when (mode) {
            LoginViewModel.SignupMode.EMAIL -> phoneInput.apply {
                error = getString(R.string.invalid_email_address)
                requestFocus()
            }
            LoginViewModel.SignupMode.PHONE -> phoneInput.apply {
                error = getString(R.string.invalid_phone_number)
                requestFocus()
            }
        }
    }

    private fun signInByGoogle() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, GOOGLE_SIGNIN_REQ_CODE)
    }

}
