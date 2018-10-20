package com.hmomeni.canto.entities

import com.google.gson.annotations.SerializedName

data class CoverPhoto(
        @field:SerializedName("link")
        val link: String,
        @field:SerializedName("id")
        val id: Int
)