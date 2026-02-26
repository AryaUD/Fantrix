package com.example.fantrix.Viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantrix.Service.ApiClient
import com.example.fantrix.Service.FootballEvent
import com.example.fantrix.Service.FootballServices.Fixture
import com.example.fantrix.Service.FootballServices.FootballApiService
import com.example.fantrix.Service.FootballServices.FootballLineup
import com.example.fantrix.Service.FootballServices.FootballTeamStats
import kotlinx.coroutines.launch

class FootballViewModel : ViewModel() {

    private val api =
        ApiClient.footballRetrofit.create(FootballApiService::class.java)

    /* ================= FIXTURES ================= */

    val fixtures = mutableStateOf<List<Fixture>>(emptyList())
    val isLoading = mutableStateOf(false)

    fun loadFixtures(league: Int? = null) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = api.getFixtures(
                    season = 2023,
                    league = league      // ✅ FIXED (THIS LINE)
                )
                fixtures.value = response.response
            } catch (e: Exception) {
                e.printStackTrace()
                fixtures.value = emptyList()
            } finally {
                isLoading.value = false
            }
        }
    }

    /* ================= MATCH DETAILS ================= */

    val matchDetails = mutableStateOf<Fixture?>(null)
    val matchStats = mutableStateOf<List<FootballTeamStats>>(emptyList())

    fun loadMatchDetails(fixtureId: Int) {
        viewModelScope.launch {
            try {
                matchDetails.value =
                    api.getFixtureById(fixtureId).response.firstOrNull()

                matchStats.value =
                    api.getFixtureStats(fixtureId).response
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /* ================= MATCH EVENTS ================= */

    val matchEvents = mutableStateOf<List<FootballEvent>>(emptyList())

    fun loadMatchEvents(fixtureId: Int) {
        viewModelScope.launch {
            try {
                val response = api.getMatchEvents(fixtureId)
                matchEvents.value = response.response
            } catch (e: Exception) {
                e.printStackTrace()
                matchEvents.value = emptyList()
            }
        }
    }

    /* ================= LINEUPS ================= */

    private val _matchLineups = mutableStateOf<List<FootballLineup>>(emptyList())
    val matchLineups: State<List<FootballLineup>> = _matchLineups

    fun loadMatchLineups(fixtureId: Int) {
        viewModelScope.launch {
            try {
                val response = api.getMatchLineups(fixtureId)
                _matchLineups.value = response.response
            } catch (e: Exception) {
                e.printStackTrace()
                _matchLineups.value = emptyList()
            }
        }
    }
}
