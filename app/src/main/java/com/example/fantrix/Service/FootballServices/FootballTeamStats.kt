package com.example.fantrix.Service.FootballServices

data class FootballTeamStats(
    val team: FootballTeamInfo,
    val statistics: List<FootballStatItem>
)

data class FootballTeamInfo(
    val id: Int,
    val name: String,
    val logo: String
)

data class FootballStatItem(
    val type: String,
    val value: Any?
)
