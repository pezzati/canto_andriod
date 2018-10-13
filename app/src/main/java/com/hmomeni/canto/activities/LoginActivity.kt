package com.hmomeni.canto.activities

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.hmomeni.canto.R
import com.hmomeni.canto.utils.MyAnimatorListener
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private var step = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        phoneBtn.setOnClickListener {
            when (step) {
                0 -> goToPhoneInput()
                1 -> goToCodeInput()
                2 -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun goToPhoneInput() {
        phoneInputWrapper.viewTreeObserver.addOnGlobalLayoutListener {
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
            }
        }
        phoneInputWrapper.visibility = View.VISIBLE
    }

    private fun goToCodeInput() {
        codeInputWrapper.viewTreeObserver.addOnGlobalLayoutListener {
            if (codeInputWrapper.measuredHeight > 0 && step == 1) {
                step = 2
                phoneInputWrapper.animate().alpha(0f)
                codeInputWrapper.animate().alpha(1f)
                phoneBtn.animate().y(codeInputWrapper.y + codeInputWrapper.height + 50).setListener(object : MyAnimatorListener() {
                    override fun onAnimationEnd(animation: Animator?) {
                        phoneInputWrapper.visibility = View.GONE
                    }
                })
            }
        }
        codeInputWrapper.visibility = View.VISIBLE
    }
}
