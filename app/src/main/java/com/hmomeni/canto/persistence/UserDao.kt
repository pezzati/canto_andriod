package com.hmomeni.canto.persistence

import androidx.room.*
import com.hmomeni.canto.entities.User
import io.reactivex.Single

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(user: User)

    @Query("SELECT * FROM User LIMIT 1")
    fun getCurrentUser(): Single<User>

    @Update
    fun updateUser(user: User)
}