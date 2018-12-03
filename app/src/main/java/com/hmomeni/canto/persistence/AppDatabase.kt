package com.hmomeni.canto.persistence

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import com.hmomeni.canto.entities.FullPost
import com.hmomeni.canto.entities.Project
import com.hmomeni.canto.entities.Track
import com.hmomeni.canto.persistence.typeconvertors.PostTypeConvertor

@Database(
        entities = [Project::class, Track::class, FullPost::class],
        version = 1
)
@TypeConverters(PostTypeConvertor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDap(): ProjectDao
    abstract fun trackDap(): TrackDao
    abstract fun postDao(): PostDao
}