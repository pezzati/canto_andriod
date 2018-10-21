package com.hmomeni.canto.entities

import com.google.gson.annotations.SerializedName

data class Post(
        @field:SerializedName("id")
        val id: Int,
        @field:SerializedName("name")
        val name: String? = null,
        @field:SerializedName("cover_photo")
        val coverPhoto: CoverPhoto? = null,
        @field:SerializedName("is_favorite")
        val isFavorite: Boolean = false,
        @field:SerializedName("like")
        val like: Int = 0,
        @field:SerializedName("artist")
        val artist: Artist? = null,
        @field:SerializedName("popularity_rate")
        val popularityRate: Int = 0,
        @field:SerializedName("link")
        val link: String? = null,
        @field:SerializedName("description")
        val description: String? = null,
        @field:SerializedName("type")
        val type: String? = null,
        @field:SerializedName("content")
        val content: String? = null,
        @field:SerializedName("tags")
        val tags: List<String>? = null,
        @field:SerializedName("is_premium")
        val isPremium: Boolean = false,
        @field:SerializedName("genre")
        val genre: Genre? = null,
        @field:SerializedName("created_date")
        val createdDate: String = "",
        @field:SerializedName("liked_it")
        val likedIt: Boolean = false
)