package com.hmomeni.canto.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.hmomeni.canto.api.Api
import dagger.Module
import dagger.Provides
import org.mockito.Mockito.mock
import javax.inject.Singleton

@Module
class TestApiModule {
    @Provides
    @Singleton
    fun providesGson(): Gson =
            GsonBuilder()
                    .create()

    @Provides
    @Singleton
    fun providesApi(): Api {
        val api = mock(Api::class.java)
        return api
    }
}