package com.hmomeni.canto.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.hmomeni.canto.utils.AvatarTypeConverters

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
        @SerializedName("premium_days")
        var premiumDays: Int,
        @SerializedName("coins")
        var coins: Int,
        @TypeConverters(AvatarTypeConverters::class)
        @SerializedName("avatar")
        var avatar: Avatar?
)