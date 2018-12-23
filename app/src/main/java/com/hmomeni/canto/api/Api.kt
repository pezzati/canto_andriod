package com.hmomeni.canto.api

import com.google.gson.JsonObject
import com.hmomeni.canto.entities.*
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.http.*

interface Api {
    @POST("handshake")
    fun handshake(@Body requestBody: RequestBody): Single<JsonObject>

    @POST("user/signup")
    fun signUp(@Body requestBody: RequestBody): Completable

    @POST("user/profile/verify")
    fun verify(@Body requestBody: RequestBody): Single<JsonObject>

    @GET("v2/song/home")
    fun getHomeFeed(): Single<List<HomeFeed>>

    @GET("analysis/banners")
    fun getBanners(): Single<ApiResponse<List<Banner>>>

    @GET("song/genre")
    fun getGenres(): Single<ApiResponse<List<Genre>>>

    @GET
    fun getGenrePosts(@Url path: String): Single<ApiResponse<List<Post>>>

    @GET("song/karaokes/search")
    fun searchInGenres(@Query("key") query: String): Single<ApiResponse<List<Post>>>

    @GET("song/posts/{postId}")
    fun getSinglePost(@Path("postId") postId: Int): Single<FullPost>

}