package com.hmomeni.canto.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hmomeni.canto.entities.User
import io.reactivex.Single

@Dao
interface UserDao {
    @Insert
    fun insert(user: User)

    @Query("SELECT * FROM User WHERE current = 1")
    fun getCurrentUser(): Single<User>
}