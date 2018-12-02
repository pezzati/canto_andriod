package com.hmomeni.canto.persistence

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import com.hmomeni.canto.entities.Project
import com.hmomeni.canto.entities.Track

@Database(
        entities = [Project::class, Track::class],
        version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDap(): ProjectDao
    abstract fun trackDap(): TrackDao
}