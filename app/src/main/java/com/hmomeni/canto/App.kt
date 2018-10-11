package com.hmomeni.canto

import android.app.Application
import com.hmomeni.canto.di.ApiModule
import com.hmomeni.canto.di.AppModule
import com.hmomeni.canto.di.DIComponent
import com.hmomeni.canto.di.DaggerDIComponent

class App : Application() {

    lateinit var di: DIComponent
    override fun onCreate() {
        super.onCreate()
        di = DaggerDIComponent.builder()
                .appModule(AppModule(this))
                .apiModule(ApiModule())
                .build()
    }
}