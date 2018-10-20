package com.hmomeni.canto.entities

import com.google.gson.annotations.SerializedName

data class Banner(
        @field:SerializedName("file")
        val file: String,
        @field:SerializedName("content_type")
        val contentType: String,
        @field:SerializedName("link")
        val link: String,
        @field:SerializedName("description")
        val description: String,
        @field:SerializedName("title")
        val title: String
)