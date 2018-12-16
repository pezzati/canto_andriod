package com.hmomeni.canto.persistence

import android.arch.persistence.room.*
import com.hmomeni.canto.entities.FullPost
import io.reactivex.Single

@Dao
interface PostDao {
    @Insert
    fun insert(post: FullPost)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(post: FullPost)

    @Update
    fun update(post: FullPost)

    @Delete
    fun delete(post: FullPost)

    @Query("SELECT * FROM FullPost WHERE id = :id")
    fun getPost(id: Long): Single<FullPost>
}