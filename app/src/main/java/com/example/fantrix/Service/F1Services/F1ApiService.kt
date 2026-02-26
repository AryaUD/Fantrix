package com.example.fantrix.Service.F1Services

import retrofit2.http.GET
import retrofit2.http.Query

interface F1ApiService {

    // Upcoming / last races
    @GET("races")
    suspend fun getRaces(
        @Query("season") season: Int,
        @Query("competition") competition: Int? = null,
        @Query("next") next: Int? = null,
        @Query("last") last: Int? = null,
        @Query("timezone") timezone: String = "Asia/Kolkata"
    ): F1RacesResponse

    // Driver standings for a season
    @GET("rankings/drivers")
    suspend fun getDriverStandings(
        @Query("season") season: Int
    ): F1RankingsResponse

    // Team standings for a season
    @GET("rankings/teams")
    suspend fun getTeamStandings(
        @Query("season") season: Int
    ): F1RankingsResponse

    // Circuits (for future race detail screen)
    @GET("circuits")
    suspend fun getCircuits(): F1CircuitsResponse

    // Drivers search/profile
    @GET("drivers")
    suspend fun getDrivers(
        @Query("search") search: String
    ): F1DriversResponse

    // Teams search/profile
    @GET("teams")
    suspend fun getTeams(
        @Query("search") search: String? = null
    ): F1TeamsResponse
}