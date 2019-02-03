package com.hmomeni.canto.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.hmomeni.canto.utils.FA_LANG
import com.hmomeni.canto.utils.LogoutEvent
import com.hmomeni.canto.utils.UserSession
import com.hmomeni.canto.utils.app
import com.pixplicity.easyprefs.library.Prefs
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import java.util.*
import javax.inject.Inject

open class BaseActivity : AppCompatActivity() {
    @Inject
    lateinit var logoutEvent: PublishProcessor<LogoutEvent>
    @Inject
    lateinit var userSession: UserSession

    private var logoutDisposable: Disposable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app().di.inject(this)
        FirebaseAnalytics.getInstance(this)
                .setCurrentScreen(this, this.javaClass.simpleName, null)

        logoutDisposable = logoutEvent
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Prefs.remove("token")

                    userSession.token = null
                    userSession.user = null

                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
    }

    override fun onResume() {
        applyLang()
        super.onResume()
    }

    override fun onDestroy() {
        logoutDisposable?.dispose()
        super.onDestroy()
    }

    private fun applyLang() {
        val dm = resources.displayMetrics
        val conf = resources.configuration
        val locale = Locale(FA_LANG.toLowerCase())
        Locale.setDefault(locale)
        conf.setLocale(locale)
        resources.updateConfiguration(conf, dm)
    }
}