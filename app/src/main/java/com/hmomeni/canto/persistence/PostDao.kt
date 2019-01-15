package com.hmomeni.canto.persistence

import androidx.room.*
import com.hmomeni.canto.entities.FullPost
import io.reactivex.Single

@Dao
interface PostDao {
    @Insert
    fun insert(post: FullPost)

    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun insertIgnore(post: FullPost): Long

    @Update
    fun update(post: FullPost)

    @Delete
    fun delete(post: FullPost)

    @Query("SELECT * FROM FullPost WHERE id = :id")
    fun getPost(id: Long): Single<FullPost>
}