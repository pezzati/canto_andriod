package com.hmomeni.canto.di

import androidx.room.Room
import com.hmomeni.canto.App
import com.hmomeni.canto.persistence.*
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RoomModule(app: App) {

    private val appDatabase = Room.databaseBuilder(app.applicationContext, AppDatabase::class.java, "canto")

            .allowMainThreadQueries()
            .build()

    @Provides
    @Singleton
    fun providesAppDatabase(): AppDatabase = appDatabase

    @Provides
    fun providesProjectDao(): ProjectDao = appDatabase.projectDao()

    @Provides
    fun providesTrackDao(): TrackDao = appDatabase.trackDao()

    @Provides
    fun providesPostDao(): PostDao = appDatabase.postDao()

    @Provides
    fun providesUserDao(): UserDao = appDatabase.userDao()
}

