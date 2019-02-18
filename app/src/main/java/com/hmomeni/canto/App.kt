package com.hmomeni.canto

import android.app.Application
import com.google.gson.Gson
import com.hmomeni.canto.di.AppModule
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.di.DaggerDIComponent
import com.hmomeni.canto.persistence.UserDao
import com.hmomeni.canto.utils.UserSession
import com.pixplicity.easyprefs.library.Prefs
import timber.log.Timber
import javax.inject.Inject

open class App : Application() {

    companion object {
        lateinit var gson: Gson
    }

    @Inject
    lateinit var iGson: Gson
    @Inject
    lateinit var userSession: UserSession
    @Inject
    lateinit var userDao: UserDao


    open lateinit var di: DIComponent
    override fun onCreate() {
        super.onCreate()

        Prefs.Builder().setContext(this).setUseDefaultSharedPreference(true).build()

        Timber.plant(Timber.DebugTree())

        di = DaggerDIComponent.builder()
                .applicationContext(this)
                .appModule(AppModule(this))
                .build()

        di.inject(this)
        gson = iGson

        if (Prefs.contains("token")) {
            userSession.token = Prefs.getString("token", null)
        }

    }
}