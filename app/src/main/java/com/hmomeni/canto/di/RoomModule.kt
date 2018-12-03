package com.hmomeni.canto.di

import android.arch.persistence.room.Room
import com.hmomeni.canto.App
import com.hmomeni.canto.persistence.AppDatabase
import com.hmomeni.canto.persistence.PostDao
import com.hmomeni.canto.persistence.ProjectDao
import com.hmomeni.canto.persistence.TrackDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RoomModule(app: App) {

    private val appDatabase = Room.databaseBuilder(app.applicationContext, AppDatabase::class.java, "canto")
            .build()

    @Provides
    @Singleton
    fun providesAppDatabase(): AppDatabase = appDatabase

    @Provides
    fun providesProjectDao(): ProjectDao = appDatabase.projectDap()

    @Provides
    fun providesTrackDao(): TrackDao = appDatabase.trackDap()

    @Provides
    fun providesPostDao(): PostDao = appDatabase.postDao()
}

