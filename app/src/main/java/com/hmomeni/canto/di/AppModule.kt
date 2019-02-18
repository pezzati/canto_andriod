package com.hmomeni.canto.di

import android.app.Application
import com.hmomeni.canto.App
import com.hmomeni.canto.utils.DownloadEvent
import com.hmomeni.canto.utils.LogoutEvent
import com.hmomeni.canto.utils.navigation.NavEvent
import dagger.Module
import dagger.Provides
import io.reactivex.processors.PublishProcessor
import javax.inject.Singleton

@Module
class AppModule(private val app: Application) {
    @Provides
    fun providesApp(): App = app as App

    @Provides
    @Singleton
    fun providesNavEvents(): PublishProcessor<NavEvent> = PublishProcessor.create()

    @Provides
    @Singleton
    fun providesProgressEvents(): PublishProcessor<DownloadEvent> = PublishProcessor.create()

    @Provides
    @Singleton
    fun providesLogoutEvent(): PublishProcessor<LogoutEvent> = PublishProcessor.create()
}