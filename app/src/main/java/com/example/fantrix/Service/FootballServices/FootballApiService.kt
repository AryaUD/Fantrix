package com.example.fantrix.Service.FootballServices

import retrofit2.http.GET
import retrofit2.http.Query
import com.example.fantrix.Service.FootballEventsResponse

interface FootballApiService {

    /* ---------------- FIXTURES ---------------- */

    @GET("fixtures")
    suspend fun getFixtures(
        @Query("league") league: Int?,
        @Query("season") season: Int
    ): FixturesResponse


    @GET("fixtures")
    suspend fun getFixtureById(
        @Query("id") fixtureId: Int
    ): FixturesResponse


    /* ---------------- STATS ---------------- */

    @GET("fixtures/statistics")
    suspend fun getFixtureStats(
        @Query("fixture") fixtureId: Int
    ): FootballStatsResponse


    /* ---------------- EVENTS (TIMELINE + GOALS) ---------------- */

    @GET("fixtures/events")
    suspend fun getMatchEvents(
        @Query("fixture") fixtureId: Int
    ): FootballEventsResponse


    /* ---------------- LINEUPS ---------------- */

    @GET("fixtures/lineups")
    suspend fun getMatchLineups(
        @Query("fixture") fixtureId: Int
    ): FootballLineupsResponse


    /* ---------------- STANDINGS ---------------- */

    @GET("standings")
    suspend fun getStandings(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): StandingsResponse
}
