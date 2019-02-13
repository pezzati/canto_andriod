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

    @POST("user/google_signup")
    fun googleSignIn(@Body requestBody: RequestBody): Single<JsonObject>

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

    @POST("/song/posts/{postId}/sing/")
    fun sing(@Path("postId") postId: Int): Single<JsonObject>

    @GET("finance/packages")
    fun getPaymentPacks(): Single<ApiResponse<List<PaymentPackage>>>

    @POST("finance/purchase")
    fun createInvoice(@Body requestBody: RequestBody): Single<JsonObject>

    @POST("finance/bazzar_paymnet")
    fun verifyPayment(@Body requestBody: RequestBody): Single<JsonObject>

    @POST("song/posts/{postId}/buy/")
    fun purchaseSong(@Path("postId") postId: Int): Single<JsonObject>

    @GET("user/avatar")
    fun getAvatarList(@Query("page") page: Int = 1): Single<ApiResponse<List<Avatar>>>

    @GET("user/profile")
    fun getUserInfo(): Single<User>

    @POST("user/profile/")
    fun updateUserInfo(@Body requestBody: RequestBody): Single<User>

    @POST("/finance/giftcodes/validate/")
    fun validateGiftCode(@Body requestBody: RequestBody): Completable

    @POST("/finance/giftcodes/apply/")
    fun applyGiftCode(@Body requestBody: RequestBody): Completable

    @POST("/analysis/actions/")
    fun syncActions(@Body requestBody: RequestBody): Completable

}