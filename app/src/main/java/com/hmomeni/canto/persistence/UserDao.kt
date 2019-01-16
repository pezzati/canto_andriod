package com.hmomeni.canto.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hmomeni.canto.entities.User
import io.reactivex.Single

@Dao
interface UserDao {
    @Insert
    fun insert(user: User)

    @Query("SELECT * FROM User LIMIT 1")
    fun getCurrentUser(): Single<User>

    @Update
    fun updateUser(user: User)
}