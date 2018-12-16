package com.hmomeni.canto.persistence

import android.arch.persistence.room.*
import com.hmomeni.canto.entities.TRACK_TYPE_FINAL
import com.hmomeni.canto.entities.Track
import io.reactivex.Single

@Dao
interface TrackDao {
    @Insert
    fun insert(track: Track): Long

    @Update
    fun update(track: Track)

    @Delete
    fun delete(track: Track)

    @Query("SELECT * FROM Track WHERE projectId = :projectId")
    fun fetchTracksForProject(projectId: Long): Single<List<Track>>

    @Query("SELECT * FROM Track WHERE projectId = :projectId AND type = $TRACK_TYPE_FINAL")
    fun fetchFinalTrackForProject(projectId: Long): Single<Track>
}