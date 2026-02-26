package com.example.fantrix.Viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantrix.Service.ApiClient
import com.example.fantrix.Service.F1Services.F1ApiService
import com.example.fantrix.Service.F1Services.F1Ranking
import kotlinx.coroutines.launch

class F1StandingsViewModel : ViewModel() {

    private val api =
        ApiClient.f1Retrofit.create(F1ApiService::class.java)

    val driverStandings = mutableStateOf<List<F1Ranking>>(emptyList())
    val teamStandings = mutableStateOf<List<F1Ranking>>(emptyList())
    val isLoading = mutableStateOf(false)

    fun loadDriverStandings(season: Int) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = api.getDriverStandings(season)
                driverStandings.value = response.response ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                driverStandings.value = emptyList()
            } finally {
                isLoading.value = false
            }
        }
    }

    fun loadTeamStandings(season: Int) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = api.getTeamStandings(season)
                teamStandings.value = response.response ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                teamStandings.value = emptyList()
            } finally {
                isLoading.value = false
            }
        }
    }
}