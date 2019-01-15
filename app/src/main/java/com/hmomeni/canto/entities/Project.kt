package com.hmomeni.canto.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName


const val PROJECT_TYPE_SINGING = 1
const val PROJECT_TYPE_DUBSMASH = 2
const val PROJECT_TYPE_KARAOKE = 3

@Entity
class Project(
        @PrimaryKey
        @SerializedName("id")
        var id: Long? = null,
        @SerializedName("name")
        var name: String,
        @SerializedName("type")
        var type: Int,
        @SerializedName("post_id")
        var postId: Long
) {
    @Ignore
    @SerializedName("tracks")
    var tracks: List<Track> = listOf()
}