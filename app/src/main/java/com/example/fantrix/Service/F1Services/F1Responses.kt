package com.example.fantrix.Service.F1Services

data class F1ApiResponse<T>(
    val get: String?,
    val parameters: Map<String, Any>?,
    val errors: List<Any>?,
    val results: Int?,
    val response: List<T>?
)

typealias F1RacesResponse = F1ApiResponse<F1Race>
typealias F1RankingsResponse = F1ApiResponse<F1Ranking>
typealias F1DriversResponse = F1ApiResponse<Driver>
typealias F1TeamsResponse = F1ApiResponse<Team>
typealias F1CircuitsResponse = F1ApiResponse<Circuit>