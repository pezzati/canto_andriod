package com.hmomeni.canto.activities

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.hmomeni.canto.utils.FA_LANG
import java.util.*

open class BaseFullActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val dm = resources.displayMetrics
        val conf = resources.configuration
        val locale = Locale(FA_LANG.toLowerCase())
        Locale.setDefault(locale)
        conf.setLocale(locale)
        resources.updateConfiguration(conf, dm)
        super.onCreate(savedInstanceState)
        val uiOptions = window.decorView.systemUiVisibility
        val newUiOptions = uiOptions or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        window.decorView.systemUiVisibility = newUiOptions
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}