package com.hmomeni.canto.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hmomeni.canto.App
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.utils.BASE_URL
import com.pixplicity.easyprefs.library.Prefs
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Singleton

@Module
class ApiModule {
    @Provides
    @Singleton
    fun providesGson(): Gson =
            GsonBuilder()
                    .create()

    @Provides
    @Singleton
    fun providesRetrofit(app: App, gson: Gson): Retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(
                    OkHttpClient.Builder()
//                            .writeTimeout(30, TimeUnit.SECONDS)
//                            .readTimeout(30, TimeUnit.SECONDS)
                            .addNetworkInterceptor {
                                val builder = it.request().newBuilder()
                                val token = Prefs.getString("token", "")
                                if (token.isNotEmpty()) {
                                    builder.addHeader("USERTOKEN", token)
                                }
                                it.proceed(builder.build())
                            }
                            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                            .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.HEADERS))
                            .build()
            )
            .build()

    @Provides
    @Singleton
    fun providesApi(retrofit: Retrofit): Api = retrofit.create(Api::class.java)
}