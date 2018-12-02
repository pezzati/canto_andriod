package com.hmomeni.canto.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity
class Project(
        @PrimaryKey
        @SerializedName("id")
        var id: Int,
        @SerializedName("name")
        var name: String,
        @SerializedName("type")
        var type: Int
) {
    @Ignore
    @SerializedName("tracks")
    var tracks: List<Track> = listOf()
}