package com.example.fantrix.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fantrix.Service.CricketServices.CricketMatch
import com.example.fantrix.Service.CricketServices.Score
import com.example.fantrix.Service.CricketServices.TeamInfo
import com.example.fantrix.Viewmodel.CricketViewModel

@Composable
fun CricketScreen(
    navController: NavController,
    viewModel: CricketViewModel
) {
    val matches by viewModel.matches.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    var selectedTab by remember { mutableStateOf("Live") }

    LaunchedEffect(Unit) {
        viewModel.loadMatches("e3e18e76-265d-4b7b-a4b1-62cbc325b989")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cricket Matches", style = MaterialTheme.typography.titleMedium)

            IconButton(onClick = {
                viewModel.loadMatches("e3e18e76-265d-4b7b-a4b1-62cbc325b989")
            }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        CricketTabs(selected = selectedTab, onSelected = { selectedTab = it })
        Spacer(Modifier.height(4.dp))

        val filteredMatches = matches.filter { match ->
            when (selectedTab) {
                "Live" -> match.matchStarted && !match.matchEnded
                "Upcoming" -> !match.matchStarted
                else -> match.matchEnded
            }
        }

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            filteredMatches.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matches available")
                }
            }

            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredMatches) { match ->
                        CricketMatchCard(match)
                    }
                }
            }
        }
    }
}

@Composable
fun CricketTabs(selected: String, onSelected: (String) -> Unit) {
    val tabs = listOf("Live", "Upcoming", "Finished")

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(tabs) { tab ->
            FilterChip(
                selected = selected == tab,
                onClick = { onSelected(tab) },
                label = { Text(tab) }
            )
        }
    }
}

@Composable
fun CricketMatchCard(match: CricketMatch) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(match.name, fontWeight = FontWeight.Bold)
                if (match.matchStarted && !match.matchEnded) LiveBadge()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TeamBlock(match.teamInfo.getOrNull(0))
                ScoreBlock(match.score)
                TeamBlock(match.teamInfo.getOrNull(1), true)
            }

            Text(match.venue ?: "Venue TBA", style = MaterialTheme.typography.bodySmall)
            Text(match.status, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TeamBlock(team: TeamInfo?, alignEnd: Boolean = false) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        if (team != null) {
            AsyncImage(model = team.img, contentDescription = null, modifier = Modifier.size(36.dp))
            Text(team.shortname)
        }
    }
}

@Composable
fun ScoreBlock(scores: List<Score>?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (!scores.isNullOrEmpty()) {
            scores.take(2).forEach { score ->
                Text("${score.r}/${score.w} (${score.o} ov)")
            }
        } else {
            Text("VS", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun LiveBadge() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            "LIVE",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onError,
            fontWeight = FontWeight.Bold
        )
    }
}