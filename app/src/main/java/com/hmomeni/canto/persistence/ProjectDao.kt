package com.hmomeni.canto.persistence

import android.arch.persistence.db.SupportSQLiteQuery
import android.arch.persistence.room.*
import com.hmomeni.canto.entities.CompleteProject
import com.hmomeni.canto.entities.Project
import com.hmomeni.canto.entities.TRACK_TYPE_FINAL
import io.reactivex.Single

@Dao
interface ProjectDao {
    @Insert
    fun insert(project: Project): Long

    @Update
    fun update(project: Project)

    @Delete
    fun delete(project: Project)

    @Query("SELECT * FROM Project")
    fun fetchProjects(): Single<List<Project>>

    @Query("SELECT F.*, T.filePath, T.ratio FROM Project as P INNER JOIN FullPost as F ON F.id = P.postId INNER JOIN Track as T ON T.projectId = P.id WHERE T.type = $TRACK_TYPE_FINAL")
    fun fetchCompleteProjects(): Single<List<CompleteProject>>

    @RawQuery
    fun getNextProjectId(query: SupportSQLiteQuery): Int

}