package com.hmomeni.canto.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hmomeni.canto.BuildConfig
import com.hmomeni.canto.api.Api
import com.hmomeni.canto.utils.BASE_URL
import com.hmomeni.canto.utils.LogoutEvent
import com.hmomeni.canto.utils.UserSession
import dagger.Module
import dagger.Provides
import io.reactivex.processors.PublishProcessor
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
    fun providesRetrofit(gson: Gson, userSession: UserSession, logoutEvents: PublishProcessor<LogoutEvent>): Retrofit = Retrofit.Builder()
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
                                if (userSession.isUser()) {
                                    builder.addHeader("USERTOKEN", userSession.token!!)
                                }
                                builder.addHeader("deviceType", "android")
                                builder.addHeader("market", BuildConfig.market)
                                it.proceed(builder.build())
                            }
                            .addInterceptor {
                                return@addInterceptor it.proceed(it.request()).also {
                                    if (it.code() == 401) {
                                        logoutEvents.onNext(LogoutEvent())
                                    }
                                }
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