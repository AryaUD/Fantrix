package com.example.fantrix.Service.F1Services

class F1Repository(private val api: F1ApiService) {

    suspend fun getUpcomingRaces(season: Int, next: Int = 5) =
        api.getRaces(season = season, next = next)

    suspend fun getLastRaces(season: Int, last: Int = 5) =
        api.getRaces(season = season, last = last)

    suspend fun getDriverStandings(season: Int) =
        api.getDriverStandings(season)

    suspend fun getTeamStandings(season: Int) =
        api.getTeamStandings(season)

    suspend fun getCircuits() =
        api.getCircuits()
}