package com.example.fantrix.Service.CricketServices

data class CricketMatchesResponse(
    val apikey: String?,
    val data: List<CricketMatch>,
    val status: String,
    val info: CricketInfo
)

data class CricketMatch(
    val id: String,
    val name: String,
    val matchType: String?,
    val status: String,
    val venue: String?,
    val date: String,
    val dateTimeGMT: String,
    val teams: List<String>,
    val teamInfo: List<TeamInfo>,
    val score: List<Score>?,
    val series_id: String,
    val fantasyEnabled: Boolean,
    val bbbEnabled: Boolean,
    val hasSquad: Boolean,
    val matchStarted: Boolean,
    val matchEnded: Boolean
)

data class TeamInfo(
    val name: String,
    val shortname: String,
    val img: String
)

data class Score(
    val r: Int,
    val w: Int,
    val o: Double,
    val inning: String
)

data class CricketInfo(
    val hitsToday: Int,
    val hitsUsed: Int,
    val hitsLimit: Int,
    val credits: Int,
    val server: Int,
    val offsetRows: Int,
    val totalRows: Int,
    val queryTime: Double,
    val s: Int,
    val cache: Int
)