package com.hmomeni.canto.entities

import com.google.gson.annotations.SerializedName

class VideoFeedItem(
        @SerializedName("id")
        val id: Long,
        @SerializedName("cover_photo")
        val coverPhoto: CantoFile,
        @SerializedName("content")
        val song: Song
)