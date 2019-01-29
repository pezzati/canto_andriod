package com.hmomeni.canto.activities

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import com.crashlytics.android.Crashlytics
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.*
import com.hmomeni.canto.vms.LoginViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.login_buttons_wrapper.*
import kotlinx.android.synthetic.main.login_code_input_wrapper.*
import kotlinx.android.synthetic.main.login_phone_input_wrapper.*
import retrofit2.HttpException
import timber.log.Timber
import java.util.regex.Pattern


class LoginActivity : BaseActivity(), View.OnClickListener {


    private lateinit var viewModel: LoginViewModel
    private val compositeDisposable = CompositeDisposable()

    private var step = 0
    private var mGoogleApiClient: GoogleApiClient? = null

    private val GOOGLE_SIGNIN_REQ_CODE = 4543

    private var buttonsInflated = false
    private var phoneInflated = false
    private var codeInflated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val clientId = getString(R.string.google_signin_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientId)
                .requestEmail()
                .build()

        if (mGoogleApiClient == null) {
            mGoogleApiClient = GoogleApiClient.Builder(this)
                    .enableAutoManage(this) { googleBtn.visibility = View.GONE }
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build()
        }

        viewModel = ViewModelProviders.of(this, ViewModelFactory(app()))[LoginViewModel::class.java]

        setContentView(R.layout.activity_login)

        cantoWrapper.translationY += getScreenDimensions(this).height / 6

        handshake()
    }

    private fun handshake() {
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
                                    positiveListener = { _, _ ->
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.second)))
                                    }).show()
                        }
                        1 -> PaymentDialog(this,
                                getString(R.string.force_update),
                                getString(R.string.update_force_rationale),
                                imageResId = R.drawable.force_update,
                                autoDismiss = false,
                                showPositiveButton = true,
                                showNegativeButton = false,
                                positiveButtonText = getString(R.string.update),
                                negativeButtonText = getString(R.string.ask_later),
                                positiveListener = { _, _ ->
                                    if (!p.second.isNullOrEmpty()) {
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.second)))
                                    }
                                }).apply { setCanceledOnTouchOutside(false) }.show()
                    }
                }, {
                    PaymentDialog(this,
                            getString(R.string.no_network),
                            getString(R.string.please_check_internet),
                            imageResId = R.drawable.no_internet_conation,
                            showPositiveButton = false,
                            showNegativeButton = true,
                            negativeButtonText = getString(R.string.ok),
                            negativeListener = {
                                handshake()
                            }).show()
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
            try {
                val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                if (result.isSuccess) {
                    val account = result.signInAccount
                    val progressDialog = ProgressDialog(this)
                    Timber.d("IDTOKEN: %s", account?.idToken)
                    viewModel.googleSignIn(account!!.idToken!!)
                            .iomain()
                            .doOnSubscribe { progressDialog.show() }
                            .doAfterTerminate { progressDialog.dismiss() }
                            .subscribe({
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }, {
                                if (it is HttpException && it.code() == 400) {
                                    Toast.makeText(this, it.response().errorString(), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, R.string.failed_requstin_verification, Toast.LENGTH_SHORT).show()
                                }
                                Timber.e(it)
                            }).addTo(compositeDisposable)
                } else {
                    Toast.makeText(this, R.string.failed_requstin_verification, Toast.LENGTH_SHORT).show()
                    Timber.e("Code: %s, Message: %s", result.status.statusCode, result.status.statusMessage)
                }

            } catch (e: Exception) {
                Timber.e(e)
                Crashlytics.logException(e)
                Toast.makeText(this, R.string.login_by_google_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun prepareLogin() {
        if (!buttonsInflated) {
            buttonsStub.inflate()
            phoneBtn.setOnClickListener(this)
            emailBtn.setOnClickListener(this)
            googleBtn.setOnClickListener(this)
            buttonsInflated = true
        }
        val height = getScreenDimensions(this).height
        cantoWrapper.animate().translationYBy(-height / 6f)
        buttonsWrapper.postDelayed({
            buttonsWrapper.visibility = View.VISIBLE
            buttonsWrapper.animate().alpha(1f)
        }, 300)
    }

    private var modeIsEmail = false
    private fun goToPhoneInput(email: Boolean = false) {
        modeIsEmail = email
        if (!phoneInflated) {
            phoneInputStub.inflate()
            loginBtn.setOnClickListener(this)
            phoneInflated = true
        }
        phoneInput.setText("")
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

        if (!codeInflated) {
            codeInputStub.inflate()

            verifyBtn.setOnClickListener(this)
            wrongPhoneBtn.setOnClickListener(this)
            noCodeBtn.setOnClickListener(this)
            codeInflated = true
        }

        phoneNumberView.text = phoneInput.text
        noCodeBtn.isClickable = false

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

        val progressDialog = ProgressDialog(this)

        viewModel.signUp(phoneInput.text.toString())
                .iomain()
                .doOnSubscribe { progressDialog.show() }
                .doAfterTerminate { progressDialog.dismiss() }
                .subscribe({
                    goToCodeInput()
                    timerCanceled = false
                    startCountDownTimer(45)
                }, {
                    if (it is HttpException && it.code() == 400) {
                        Toast.makeText(this, it.response().errorString(), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, R.string.failed_requstin_verification, Toast.LENGTH_SHORT).show()
                    }
                    Timber.e(it)
                }).addTo(compositeDisposable)
    }

    private fun submitCode() {
        val progressDialog = ProgressDialog(this)

        viewModel.verify(codeInput.text.toString())
                .iomain()
                .doOnSubscribe { progressDialog.show() }
                .doAfterTerminate { progressDialog.dismiss() }
                .subscribe({
                    val intent = Intent(this, MainActivity::class.java)
                            .apply {
                                if (it) {
                                    putExtra("new_user", it)
                                }
                            }
                    startActivity(intent)
                    finish()
                }, {
                    if (it is HttpException && it.code() == 400) {
                        Toast.makeText(this, it.response().errorString(), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, R.string.failed_requstin_verification, Toast.LENGTH_SHORT).show()
                    }
                    Timber.e(it)
                }).addTo(compositeDisposable)
    }

    private var timerCanceled = false
    @SuppressLint("SetTextI18n")
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
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signInIntent, GOOGLE_SIGNIN_REQ_CODE)
    }

}
