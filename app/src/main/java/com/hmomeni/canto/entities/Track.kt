package com.hmomeni.canto.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import com.google.gson.annotations.SerializedName

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
        var id: Int,
        @SerializedName("project_id")
        var projectId: Int,
        @SerializedName("type")
        var type: Int,
        @SerializedName("index")
        var index: Int,
        @SerializedName("file_path")
        var filePath: String
)