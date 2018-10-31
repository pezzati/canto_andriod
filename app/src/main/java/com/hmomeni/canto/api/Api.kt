package com.hmomeni.canto.api

import com.google.gson.JsonObject
import com.hmomeni.canto.entities.ApiResponse
import com.hmomeni.canto.entities.Banner
import com.hmomeni.canto.entities.Genre
import com.hmomeni.canto.entities.Post
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.http.*

interface Api {
    @POST("user/signup")
    fun signUp(@Body requestBody: RequestBody): Completable

    @POST("user/profile/verify")
    fun verify(@Body requestBody: RequestBody): Single<JsonObject>

    @GET("analysis/banners")
    fun getBanners(): Single<ApiResponse<List<Banner>>>

    @GET("song/genre")
    fun getGenres(): Single<ApiResponse<List<Genre>>>

    @GET("song/genre/{genreId}/karaokes")
    fun getGenrePosts(@Path("genreId") genreId: Int): Single<ApiResponse<List<Post>>>

    @GET("song/karaokes/search")
    fun searchInGenres(@Query("key") query: String): Single<ApiResponse<List<Post>>>

}