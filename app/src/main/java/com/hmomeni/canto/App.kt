package com.hmomeni.canto

import android.app.Application
import com.google.gson.Gson
import com.hmomeni.canto.di.*
import com.hmomeni.canto.persistence.UserDao
import com.hmomeni.canto.utils.UserSession
import com.pixplicity.easyprefs.library.Prefs
import timber.log.Timber
import javax.inject.Inject

class App : Application() {

    companion object {
        lateinit var gson: Gson
    }

    @Inject
    lateinit var iGson: Gson
    @Inject
    lateinit var userSession: UserSession
    @Inject
    lateinit var userDao: UserDao

    lateinit var di: DIComponent
    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        di = DaggerDIComponent.builder()
                .appModule(AppModule(this))
                .apiModule(ApiModule())
                .roomModule(RoomModule(this))
                .build()

        di.inject(this)
        gson = iGson

        Prefs.Builder().setContext(this).setUseDefaultSharedPreference(true).build()

        try {
            val user = userDao.getCurrentUser().blockingGet()
            userSession.user = user
        } catch (e: Exception) {

        }

    }
}