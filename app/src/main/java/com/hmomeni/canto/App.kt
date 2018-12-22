package com.hmomeni.canto

import android.app.Application
import com.google.gson.Gson
import com.hmomeni.canto.di.*
import com.hmomeni.canto.persistence.UserDao
import com.hmomeni.canto.utils.FA_LANG
import com.hmomeni.canto.utils.UserSession
import com.pixplicity.easyprefs.library.Prefs
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.*
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
        val dm = resources.displayMetrics
        val conf = resources.configuration
        val locale = Locale(FA_LANG.toLowerCase())
        Locale.setDefault(locale)
        conf.setLocale(locale)
        resources.updateConfiguration(conf, dm)

        super.onCreate()

        Prefs.Builder().setContext(this).setUseDefaultSharedPreference(true).build()

        Timber.plant(Timber.DebugTree())

        di = DaggerDIComponent.builder()
                .appModule(AppModule(this))
                .apiModule(ApiModule())
                .roomModule(RoomModule(this))
                .build()

        di.inject(this)
        gson = iGson

        if (Prefs.contains("token")) {
            userSession.token = Prefs.getString("token", null)
        }

        userDao.getCurrentUser()
                .subscribeOn(Schedulers.io())
                .subscribe({
                    userSession.user = it
                }, {
                    Timber.e(it)
                })

    }
}