package com.example.fantrix.Service.FootballServices

data class StandingsResponse(
    val response: List<StandingsWrapper>
)

data class StandingsWrapper(
    val league: LeagueStandings
)

data class LeagueStandings(
    val standings: List<List<TeamStanding>>
)

data class TeamStanding(
    val rank: Int,
    val team: StandingTeam,
    val points: Int,
    val all: StandingAll
)

data class StandingTeam(
    val name: String,
    val logo: String
)

data class StandingAll(
    val played: Int
)
