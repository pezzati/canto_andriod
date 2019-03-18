package com.hmomeni.canto.persistence

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
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

    @Query("SELECT F.*, T.filePath, T.ratio, P.id as projectId FROM Project as P INNER JOIN FullPost as F ON F.id = P.postId INNER JOIN Track as T ON T.projectId = P.id WHERE T.type = $TRACK_TYPE_FINAL ORDER BY P.id DESC")
    fun fetchCompleteProjects(): Single<List<CompleteProject>>

    @RawQuery
    fun getNextProjectId(query: SupportSQLiteQuery): Int

    @Query("SELECT * FROM Project WHERE id = :id")
    fun getProject(id: Long): Single<Project>

}