package com.hmomeni.canto.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity
data class User(
        @PrimaryKey(autoGenerate = false)
        @SerializedName("id")
        var id: Long,
        @SerializedName("username")
        var username: String,
        @SerializedName("first_name")
        var firstName: String,
        @SerializedName("last_name")
        var lastName: String,
        @SerializedName("token")
        var token: String,
        var current: Boolean = false
) {
        constructor() : this(0, "", "", "", "", false)
}