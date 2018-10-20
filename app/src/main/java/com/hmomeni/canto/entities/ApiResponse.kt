package com.hmomeni.canto.entities

import com.google.gson.annotations.SerializedName

class ApiResponse<T>(
        @SerializedName("results")
        val data: T
)