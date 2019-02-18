package com.hmomeni.canto.di

import android.app.Application
import androidx.room.Room
import com.hmomeni.canto.persistence.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RoomModule(app: Application) {

    private val appDatabase = Room.databaseBuilder(app.applicationContext, AppDatabase::class.java, "canto")
            .allowMainThreadQueries()
            .build()

    @Singleton
    @Provides
    fun providesAppDatabase(): AppDatabase = appDatabase

    @Singleton
    @Provides
    fun providesProjectDao(): ProjectDao = appDatabase.projectDao()

    @Singleton
    @Provides
    fun providesTrackDao(): TrackDao = appDatabase.trackDao()

    @Singleton
    @Provides
    fun providesPostDao(): PostDao = appDatabase.postDao()

    @Singleton
    @Provides
    fun providesUserDao(): UserDao = appDatabase.userDao()
}

