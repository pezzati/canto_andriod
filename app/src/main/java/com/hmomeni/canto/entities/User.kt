package com.hmomeni.canto.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
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
        @Transient
        var current: Boolean = false
)