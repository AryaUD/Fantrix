package com.example.fantrix.Service.FootballServices

/* ================= FIXTURES ================= */

data class FixturesResponse(
    val response: List<Fixture>
)

data class Fixture(
    val fixture: FixtureInfo,
    val league: League,
    val teams: Teams,
    val goals: Goals
)

data class FixtureInfo(
    val id: Int,
    val date: String,
    val status: Status
)

data class Status(
    val long: String
)

/* ================= LEAGUE ================= */

data class League(
    val id: Int,
    val name: String,
    val logo: String,
    val season: Int
)

/* ================= TEAMS ================= */

data class Teams(
    val home: Team,
    val away: Team
)

data class Team(
    val id: Int,
    val name: String,
    val logo: String
)

data class Goals(
    val home: Int?,
    val away: Int?
)
