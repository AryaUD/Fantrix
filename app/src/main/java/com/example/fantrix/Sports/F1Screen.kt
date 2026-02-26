package com.example.fantrix.Sports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fantrix.Viewmodel.F1StandingsViewModel
import com.example.fantrix.Viewmodel.F1ViewModel

@Composable
fun F1Screen(
    f1ViewModel: F1ViewModel = viewModel<F1ViewModel>(),
    standingsViewModel: F1StandingsViewModel = viewModel<F1StandingsViewModel>()
) {
    LaunchedEffect(Unit) {
        f1ViewModel.loadUpcomingRaces(2025)
        standingsViewModel.loadDriverStandings(2025)
    }

    val races = f1ViewModel.races.value
    val isLoading = f1ViewModel.isLoading.value
    val driverStandings = standingsViewModel.driverStandings.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Upcoming F1 Races", style = MaterialTheme.typography.titleMedium)

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn {
                items(races) { race ->
                    Text("🏁 ${race.competition?.name ?: "Race"} - ${race.circuit?.name ?: ""}")
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Top 5 Drivers", style = MaterialTheme.typography.titleMedium)

        LazyColumn {
            items(driverStandings.take(5)) { standing ->
                Text("• ${standing.driver?.name ?: "Driver"} - ${standing.points} pts")
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}