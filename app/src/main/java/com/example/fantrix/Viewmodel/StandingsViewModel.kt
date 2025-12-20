package com.example.fantrix.Viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantrix.Service.ApiClient
import com.example.fantrix.Service.FootballServices.FootballApiService
import com.example.fantrix.Service.FootballServices.TeamStanding
import kotlinx.coroutines.launch

class StandingsViewModel : ViewModel() {

    private val api =
        ApiClient.retrofit.create(FootballApiService::class.java)

    val standings = mutableStateOf<List<TeamStanding>>(emptyList())
    val isLoading = mutableStateOf(false)

    fun loadStandings(leagueId: Int, season: Int) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = api.getStandings(leagueId, season)

                standings.value =
                    response.response
                        .firstOrNull()
                        ?.league
                        ?.standings
                        ?.firstOrNull()
                        ?: emptyList()

            } catch (e: Exception) {
                e.printStackTrace()
                standings.value = emptyList()
            } finally {
                isLoading.value = false
            }
        }
    }
}
