package com.hmomeni.canto

import android.app.Application
import com.hmomeni.canto.di.AppModule
import com.hmomeni.canto.di.DaggerTestDIComponent
import com.hmomeni.canto.di.TestDIComponent
import com.pixplicity.easyprefs.library.Prefs
import timber.log.Timber

class TestApp : Application() {


    lateinit var di: TestDIComponent
    override fun onCreate() {
        super.onCreate()

        Prefs.Builder().setContext(this).setUseDefaultSharedPreference(true).build()

        Timber.plant(Timber.DebugTree())

        di = DaggerTestDIComponent.builder()
                .applicationContext(this)
                .appModule(AppModule(this))
                .build()
    }
}