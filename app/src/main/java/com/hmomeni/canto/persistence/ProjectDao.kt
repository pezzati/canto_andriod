package com.hmomeni.canto.persistence

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Update
import com.hmomeni.canto.entities.Project
import io.reactivex.Single

@Dao
interface ProjectDao {
    @Insert
    fun insert(project: Project): Single<Int>

    @Update
    fun update(project: Project)

    @Delete
    fun delete(project: Project)
}