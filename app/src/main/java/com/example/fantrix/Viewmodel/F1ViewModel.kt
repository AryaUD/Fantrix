package com.example.fantrix.Viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantrix.Service.ApiClient
import com.example.fantrix.Service.F1Services.*
import kotlinx.coroutines.launch

class F1ViewModel : ViewModel() {

    private val api =
        ApiClient.f1Retrofit.create(F1ApiService::class.java)

    /* ================= RACES ================= */

    val races = mutableStateOf<List<F1Race>>(emptyList())
    val isLoading = mutableStateOf(false)

    fun loadUpcomingRaces(season: Int) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = api.getRaces(
                    season = season,
                    next = 5
                )
                races.value = response.response ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                races.value = emptyList()
            } finally {
                isLoading.value = false
            }
        }
    }

    /* ================= DRIVER STANDINGS ================= */

    val driverStandings = mutableStateOf<List<F1Ranking>>(emptyList())

    fun loadDriverStandings(season: Int) {
        viewModelScope.launch {
            try {
                val response = api.getDriverStandings(season)
                driverStandings.value = response.response ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                driverStandings.value = emptyList()
            }
        }
    }

    /* ================= TEAM STANDINGS ================= */

    val teamStandings = mutableStateOf<List<F1Ranking>>(emptyList())

    fun loadTeamStandings(season: Int) {
        viewModelScope.launch {
            try {
                val response = api.getTeamStandings(season)
                teamStandings.value = response.response ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                teamStandings.value = emptyList()
            }
        }
    }
}