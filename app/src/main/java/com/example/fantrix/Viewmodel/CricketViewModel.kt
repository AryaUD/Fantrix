package com.example.fantrix.Viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantrix.Service.CricketServices.CricketApiClient
import com.example.fantrix.Service.CricketServices.CricketMatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CricketViewModel : ViewModel() {

    private val _matches = MutableStateFlow<List<CricketMatch>>(emptyList())
    val matches: StateFlow<List<CricketMatch>> = _matches

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun loadMatches(apiKey: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = CricketApiClient.cricketApi.getCurrentMatches(apiKey)
                _matches.value = response.data
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }
}