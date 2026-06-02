package com.corp.bookmate.settermate.di

import android.content.Context
import com.corp.bookmate.settermate.service.SettersApi
import com.corp.bookmate.settermate.service.SettersRepo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import org.intellij.lang.annotations.PrintFormat
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideContext(
        @ApplicationContext context: Context,
    ): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://www.cherrygrovesportscenter.com/")
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): SettersApi =
        retrofit.create(SettersApi::class.java)

    @Provides
    @Singleton
    fun provideRepo(api: SettersApi): SettersRepo = SettersRepo(api)
}