package com.hmomeni.canto.entities

import com.google.gson.annotations.SerializedName

data class Genre(
        @field:SerializedName("files_link")
        val filesLink: String,
        @field:SerializedName("cover_photo")
        val coverPhoto: String?,
        @field:SerializedName("link")
        val link: String,
        @field:SerializedName("name")
        val name: String,
        @field:SerializedName("liked_it")
        val likedIt: Boolean,
        val posts: List<Post>? = null
)