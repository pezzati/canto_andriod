package com.hmomeni.canto.persistence

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Update
import com.hmomeni.canto.entities.FullPost

@Dao
interface PostDao {
    @Insert
    fun insert(post: FullPost)

    @Update
    fun update(post: FullPost)

    @Delete
    fun delete(post: FullPost)
}