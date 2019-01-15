package com.hmomeni.canto.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName


const val TRACK_TYPE_AUDIO = 1
const val TRACK_TYPE_VIDEO = 2
const val TRACK_TYPE_FINAL = 3

@Entity(
        indices = [
            Index("projectId", name = "project_id_indx", unique = false)
        ],
        foreignKeys = [
            ForeignKey(
                    entity = Project::class,
                    parentColumns = ["id"],
                    childColumns = ["projectId"],
                    deferred = true
            )
        ]
)
class Track(
        @PrimaryKey
        @SerializedName("id")
        var id: Long? = null,
        @SerializedName("project_id")
        var projectId: Long,
        @SerializedName("type")
        var type: Int,
        @SerializedName("index")
        var index: Int,
        @SerializedName("file_path")
        var filePath: String,
        @SerializedName("ratio")
        var ratio: Int
)