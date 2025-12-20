package com.example.fantrix.Service

data class FootballEventsResponse(
    val response: List<FootballEvent>
)

data class FootballEvent(
    val time: EventTime,
    val team: EventTeam,
    val player: EventPlayer,
    val assist: EventPlayer?,
    val type: String,
    val detail: String
)

data class EventTime(
    val elapsed: Int?
)

data class EventTeam(
    val name: String,
    val logo: String
)

data class EventPlayer(
    val name: String?
)
