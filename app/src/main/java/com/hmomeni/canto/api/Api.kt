package com.hmomeni.canto.api

import com.google.gson.JsonObject
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST

interface Api {
    @POST("user/signup")
    fun signUp(@Body requestBody: RequestBody): Completable

    @POST("user/profile/verify")
    fun verify(@Body requestBody: RequestBody): Single<JsonObject>
}