package com.hmomeni.canto.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hmomeni.canto.entities.FullPost
import com.hmomeni.canto.entities.Project
import com.hmomeni.canto.entities.Track
import com.hmomeni.canto.entities.User
import com.hmomeni.canto.persistence.typeconvertors.PostTypeConvertor
import com.hmomeni.canto.utils.AvatarTypeConverters

@Database(
        entities = [Project::class, Track::class, FullPost::class, User::class],
        version = 1
)
@TypeConverters(PostTypeConvertor::class, AvatarTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun trackDao(): TrackDao
    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao
}