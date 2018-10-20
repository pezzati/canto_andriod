package com.hmomeni.canto.utils

import com.google.gson.Gson
import okhttp3.MediaType
import okhttp3.RequestBody

class RequestMap {
    private val map: MutableMap<String, Any> = mutableMapOf()

    fun add(key: String, value: Any): RequestMap {
        map[key] = value
        return this
    }

    fun body(): RequestBody {
        return map.toBody()
    }
}

fun Map<String, Any>.toBody(): RequestBody {
    val json = Gson().toJson(this)
    return RequestBody.create(MediaType.parse("application/json"), json)
}

fun makeMap(): RequestMap = RequestMap()