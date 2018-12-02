package com.hmomeni.canto

import android.app.Application
import com.hmomeni.canto.di.*
import com.pixplicity.easyprefs.library.Prefs
import timber.log.Timber

class App : Application() {

    lateinit var di: DIComponent
    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        di = DaggerDIComponent.builder()
                .appModule(AppModule(this))
                .apiModule(ApiModule())
                .roomModule(RoomModule(this))
                .build()
        Prefs.Builder().setContext(this).setUseDefaultSharedPreference(true).build()

    }
}