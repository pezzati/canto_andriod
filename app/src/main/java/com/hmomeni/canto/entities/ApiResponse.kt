package com.hmomeni.canto.entities

import com.google.gson.annotations.SerializedName

class ApiResponse<T>(
        @SerializedName("results")
        val data: T,
        @SerializedName("next")
        val next: String? = null,
        @SerializedName("previous")
        val previous: String? = null,
        @SerializedName("count")
        val count: Int = 0
)