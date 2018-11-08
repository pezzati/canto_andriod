package com.hmomeni.canto.entities

import com.google.gson.annotations.SerializedName

data class Owner(

        @field:SerializedName("image")
        val image: Any? = null,

        @field:SerializedName("gender")
        val gender: Int? = null,

        @field:SerializedName("birth_date")
        val birthDate: String? = null,

        @field:SerializedName("mobile")
        val mobile: String? = null,

        @field:SerializedName("bio")
        val bio: String? = null,

        @field:SerializedName("last_name")
        val lastName: String? = null,

        @field:SerializedName("follower_count")
        val followerCount: Int? = null,

        @field:SerializedName("is_premium")
        val isPremium: Boolean? = null,

        @field:SerializedName("premium_days")
        val premiumDays: Int? = null,

        @field:SerializedName("following_count")
        val followingCount: Int? = null,

        @field:SerializedName("is_public")
        val isPublic: Boolean? = null,

        @field:SerializedName("post_count")
        val postCount: Int? = null,

        @field:SerializedName("first_name")
        val firstName: String? = null,

        @field:SerializedName("is_following")
        val isFollowing: Boolean? = null,

        @field:SerializedName("email")
        val email: String? = null,

        @field:SerializedName("username")
        val username: String? = null
)