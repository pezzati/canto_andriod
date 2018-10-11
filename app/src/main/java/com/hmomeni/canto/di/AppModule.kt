package com.hmomeni.canto.di

import com.hmomeni.canto.App
import dagger.Module
import dagger.Provides

@Module
class AppModule(private val app: App) {
    @Provides
    fun providesApp() = app
}