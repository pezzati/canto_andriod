package com.hmomeni.canto.entities

import com.google.gson.annotations.SerializedName

data class Post(
        @field:SerializedName("cover_photo")
        val coverPhoto: CoverPhoto? = null,
        @field:SerializedName("is_favorite")
        val isFavorite: Boolean? = null,
        @field:SerializedName("like")
        val like: Int? = null,
        @field:SerializedName("artist")
        val artist: Artist? = null,
        @field:SerializedName("popularity_rate")
        val popularityRate: Int? = null,
        @field:SerializedName("link")
        val link: String? = null,
        @field:SerializedName("description")
        val description: String? = null,
        @field:SerializedName("type")
        val type: String? = null,
        @field:SerializedName("content")
        val content: String? = null,
        @field:SerializedName("tags")
        val tags: List<Any?>? = null,
        @field:SerializedName("is_premium")
        val isPremium: Boolean? = null,
        @field:SerializedName("name")
        val name: String? = null,
        @field:SerializedName("genre")
        val genre: Genre? = null,
        @field:SerializedName("id")
        val id: Int? = null,
        @field:SerializedName("created_date")
        val createdDate: String? = null,
        @field:SerializedName("liked_it")
        val likedIt: Boolean? = null
)