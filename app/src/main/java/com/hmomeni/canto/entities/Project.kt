package com.hmomeni.canto.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import com.google.gson.annotations.SerializedName


const val PROJECT_TYPE_SINGING = 1
const val PROJECT_TYPE_DUBSMASH = 2

@Entity
class Project(
        @PrimaryKey
        @SerializedName("id")
        var id: Int,
        @SerializedName("name")
        var name: String,
        @SerializedName("type")
        var type: Int,
        @SerializedName("post_id")
        var postId: Int
) {
    @Ignore
    @SerializedName("tracks")
    var tracks: List<Track> = listOf()
}