package com.hmomeni.canto.entities

import com.google.gson.annotations.SerializedName

class HomeFeed(
        @SerializedName("name")
        val name: String,
        @SerializedName("more")
        val moreUrl: String,
        @SerializedName("data")
        val posts: List<Post>
)