package com.example.fantrix.Service.F1Services

data class F1Race(
    val id: Int,
    val name: String?,
    val date: String?,
    val time: String?,
    val status: String?,
    val type: String?,
    val circuit: Circuit?,
    val competition: Competition?
)

data class Circuit(
    val id: Int,
    val name: String?,
    val image: String?
)

data class Competition(
    val id: Int,
    val name: String?,
    val logo: String?
)

data class F1Ranking(
    val position: Int?,
    val points: Int?,
    val wins: Int?,
    val driver: Driver?,
    val team: Team?
)

data class Driver(
    val id: Int,
    val name: String?,
    val image: String?
)

data class Team(
    val id: Int,
    val name: String?,
    val logo: String?
)