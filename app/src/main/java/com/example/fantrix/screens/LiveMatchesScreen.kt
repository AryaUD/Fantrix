package com.example.fantrix.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class LiveMatch(
    val id: String,
    val name: String,
    val info: String,
    val videoUrl: String
)

@Composable
fun LiveMatchesScreen(navController: NavController) {

    val matches = listOf(
        LiveMatch(
            id = "rm_barca",
            name = "Real Madrid vs Barcelona",
            info = "La Liga • Live Now",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        ),
        LiveMatch(
            id = "city_arsenal",
            name = "Man City vs Arsenal",
            info = "Premier League • 2nd Half",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
        ),
        LiveMatch(
            id = "psg_bayern",
            name = "PSG vs Bayern",
            info = "UCL • Kick-off",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = "Live Matches",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(matches) { match ->
                MatchCard(
                    matchName = match.name,
                    matchInfo = match.info
                ) {
                    val route =
                        "live_player" +
                                "?url=${Uri.encode(match.videoUrl)}" +
                                "&name=${Uri.encode(match.name)}" +
                                "&info=${Uri.encode(match.info)}" +
                                "&id=${Uri.encode(match.id)}"

                    navController.navigate(route)
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    matchName: String,
    matchInfo: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = matchName, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = matchInfo, style = MaterialTheme.typography.bodyMedium)
        }
    }
}