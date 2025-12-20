package com.example.fantrix.Service.FootballServices

data class FootballLineup(
    val team: LineupTeam,
    val formation: String?,
    val startXI: List<LineupPlayer>,
    val substitutes: List<LineupPlayer>
)

data class LineupTeam(
    val id: Int,
    val name: String,
    val logo: String
)

data class LineupPlayer(
    val player: PlayerInfo,
    val number: Int?,
    val pos: String?,
    val grid: String?
)

data class PlayerInfo(
    val id: Int,
    val name: String,
    val photo: String?
)
