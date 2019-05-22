package com.hmomeni.canto.entities

import com.google.gson.annotations.SerializedName

class Song(
        @SerializedName("file")
        val file: CantoFile,
        @SerializedName("length")
        val length: Float,
        @SerializedName("file_url")
        val fileUrl: String,
        @SerializedName("link")
        val link: String,
        @SerializedName("karaoke")
        val karaoke: Karaoke,
        @SerializedName("thumbnail")
        val thumbnail: String
)