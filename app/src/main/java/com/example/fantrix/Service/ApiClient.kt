package com.example.fantrix.Service

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val FOOTBALL_BASE_URL = "https://v3.football.api-sports.io/"
    private const val F1_BASE_URL = "https://v1.formula-1.api-sports.io/"
    private const val API_KEY = "b7cece119b225ef97e10f90ccd2f80f1"

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("x-apisports-key", API_KEY)
            .build()
        chain.proceed(request)
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .build()

    val footballRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(FOOTBALL_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val f1Retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(F1_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}