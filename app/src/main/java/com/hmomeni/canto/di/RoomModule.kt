package com.hmomeni.canto.di

import androidx.room.Room
import com.hmomeni.canto.App
import com.hmomeni.canto.persistence.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RoomModule {

    @Singleton
    @Provides
    fun providesAppDatabase(app: App): AppDatabase = Room
            .databaseBuilder(app.applicationContext, AppDatabase::class.java, "canto")
            .allowMainThreadQueries()
            .build()

    @Singleton
    @Provides
    fun providesProjectDao(appDatabase: AppDatabase): ProjectDao = appDatabase.projectDao()

    @Singleton
    @Provides
    fun providesTrackDao(appDatabase: AppDatabase): TrackDao = appDatabase.trackDao()

    @Singleton
    @Provides
    fun providesPostDao(appDatabase: AppDatabase): PostDao = appDatabase.postDao()

    @Singleton
    @Provides
    fun providesUserDao(appDatabase: AppDatabase): UserDao = appDatabase.userDao()
}

