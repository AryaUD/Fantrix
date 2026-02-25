package com.example.fantrix.Service.CricketServices

import retrofit2.http.GET
import retrofit2.http.Query

interface CricketApiService {

    @GET("currentMatches")
    suspend fun getCurrentMatches(
        @Query("apikey") apiKey: String,
        @Query("offset") offset: Int = 0
    ): CricketMatchesResponse
}