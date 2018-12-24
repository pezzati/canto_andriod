package com.hmomeni.canto.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.hmomeni.canto.utils.FA_LANG
import java.util.*

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val dm = resources.displayMetrics
        val conf = resources.configuration
        val locale = Locale(FA_LANG.toLowerCase())
        Locale.setDefault(locale)
        conf.setLocale(locale)
        resources.updateConfiguration(conf, dm)
        super.onCreate(savedInstanceState)
    }
}