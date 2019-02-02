package com.hmomeni.canto.di

import com.hmomeni.canto.App
import com.hmomeni.canto.entities.UserInventory
import com.hmomeni.canto.persistence.UserDao
import com.hmomeni.canto.utils.DownloadEvent
import com.hmomeni.canto.utils.LogoutEvent
import com.hmomeni.canto.utils.UserSession
import com.hmomeni.canto.utils.navigation.NavEvent
import dagger.Module
import dagger.Provides
import io.reactivex.processors.PublishProcessor
import javax.inject.Singleton

@Module
class AppModule(private val app: App) {
    @Provides
    fun providesApp() = app

    @Provides
    @Singleton
    fun providesUserSession(userDao: UserDao): UserSession = UserSession(userDao)

    @Provides
    @Singleton
    fun providesNavEvents(): PublishProcessor<NavEvent> = PublishProcessor.create()

    @Provides
    @Singleton
    fun providesProgressEvents(): PublishProcessor<DownloadEvent> = PublishProcessor.create()

    @Provides
    @Singleton
    fun providesInventory(userSession: UserSession): UserInventory = UserInventory(userSession)

    @Provides
    @Singleton
    fun providesLogoutEvent(): PublishProcessor<LogoutEvent> = PublishProcessor.create()
}