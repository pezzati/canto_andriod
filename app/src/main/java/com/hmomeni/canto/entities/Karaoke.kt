package com.hmomeni.canto.entities

import com.google.gson.annotations.SerializedName

class Karaoke(
        @SerializedName("id")
        val id: Long,
        @SerializedName("artist")
        val artistName: String
) {
}