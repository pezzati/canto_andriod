package com.hmomeni.canto.di

import com.hmomeni.canto.activities.MainActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class, ApiModule::class])
interface DIComponent {
    fun inject(mainActivity: MainActivity)
}