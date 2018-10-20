package com.hmomeni.canto.entities

import com.google.gson.annotations.SerializedName

data class Artist(
        @field:SerializedName("image")
        val image: String? = null,
        @field:SerializedName("name")
        val name: String? = null,
        @field:SerializedName("link")
        val link: String? = null,
        @field:SerializedName("poems_count")
        val poemsCount: Int,
        @field:SerializedName("id")
        val id: Int
)