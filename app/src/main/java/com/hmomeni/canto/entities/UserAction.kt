package com.hmomeni.canto.entities

import com.google.gson.annotations.SerializedName

class UserAction(
        @SerializedName("action")
        val action: String,
        @SerializedName("session")
        val session: String = "",
        @SerializedName("detail")
        val detail: String = "",
        @SerializedName("timestamp")
        val timeStamp: String = (System.currentTimeMillis() / 1000).toString()
)