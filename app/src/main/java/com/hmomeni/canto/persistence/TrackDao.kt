package com.hmomeni.canto.persistence

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Update
import com.hmomeni.canto.entities.Track

@Dao
interface TrackDao {
    @Insert
    fun insert(track: Track): Long

    @Update
    fun update(track: Track)

    @Delete
    fun delete(track: Track)
}