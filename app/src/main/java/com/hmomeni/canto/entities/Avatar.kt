package com.hmomeni.canto.entities

import com.google.gson.annotations.SerializedName

class Avatar(
        @SerializedName("id")
        val id: Int,
        @SerializedName("link")
        val link: String
)