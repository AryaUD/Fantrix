package com.example.fantrix.Sports.FootballMatchDetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.fantrix.Service.FootballEvent
import com.example.fantrix.Service.FootballServices.FootballStatItem
import com.example.fantrix.Service.FootballServices.FootballTeamStats
import com.example.fantrix.Service.FootballServices.TeamStanding
import com.example.fantrix.Viewmodel.FootballViewModel
import com.example.fantrix.Viewmodel.StandingsViewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FootballMatchDetailsScreen(
    fixtureId: Int,
    navController: NavController,
    footballViewModel: FootballViewModel = viewModel(),
    standingsViewModel: StandingsViewModel = viewModel()
) {
    val match by footballViewModel.matchDetails
    val stats by footballViewModel.matchStats
    val events by footballViewModel.matchEvents

    val standings by standingsViewModel.standings
    val standingsLoading by standingsViewModel.isLoading

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Stats", "Timeline", "Lineups", "Standings")

    LaunchedEffect(fixtureId) {
        footballViewModel.loadMatchDetails(fixtureId)
        footballViewModel.loadMatchEvents(fixtureId)
        footballViewModel.loadMatchLineups(fixtureId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        match?.let { fixture ->

            LaunchedEffect(fixture.league.id) {
                standingsViewModel.loadStandings(
                    leagueId = fixture.league.id,
                    season = fixture.league.season
                )
            }

            val date = OffsetDateTime.parse(fixture.fixture.date)
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                item {
                    Text(
                        text = "${fixture.league.name} • $date",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TeamColumn(fixture.teams.home.name, fixture.teams.home.logo)

                        Text(
                            text = "${fixture.goals.home ?: 0} - ${fixture.goals.away ?: 0}",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        TeamColumn(fixture.teams.away.name, fixture.teams.away.logo)
                    }
                }

                item { Divider() }

                item {
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { Text(title) }
                            )
                        }
                    }
                }

                item {
                    when (selectedTab) {
                        0 -> StatsSection(stats)
                        1 -> MatchTimeline(events)
                        2 -> LineupsPlaceholder()
                        3 -> StandingsSection(standings, standingsLoading)
                    }
                }
            }
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

/* ================= STANDINGS UI ================= */

@Composable
fun StandingsSection(
    standings: List<TeamStanding>,
    isLoading: Boolean
) {
    when {
        isLoading -> CircularProgressIndicator()
        standings.isEmpty() -> Text("No standings available")
        else -> Column {
            Text("LEAGUE TABLE", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            standings.forEach { team ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${team.rank}")
                    Text(team.team.name)
                    Text("${team.points} pts")
                }
            }
        }
    }
}

/* ================= STATS ================= */

@Composable
fun StatsSection(stats: List<FootballTeamStats>) {
    if (stats.size < 2) {
        Text("No stats available")
        return
    }

    Column {
        Text("TEAM STATS", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        StatRow("Shots", stats[0].statistics, stats[1].statistics)
        StatRow("Shots on Goal", stats[0].statistics, stats[1].statistics)
        StatRow("Ball Possession", stats[0].statistics, stats[1].statistics)
    }
}

@Composable
fun StatRow(
    label: String,
    home: List<FootballStatItem>,
    away: List<FootballStatItem>
) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(home.find { it.type == label }?.value?.toString() ?: "-")
        Text(label)
        Text(away.find { it.type == label }?.value?.toString() ?: "-")
    }
}

/* ================= TIMELINE ================= */

@Composable
fun MatchTimeline(events: List<FootballEvent>) {
    if (events.isEmpty()) {
        Text("No timeline data")
        return
    }

    Column {
        Text("TIMELINE", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        events.forEach { event ->
            Text("${event.time.elapsed ?: "-"}' ${event.player.name ?: ""} - ${event.detail}")
        }
    }
}

/* ================= HELPERS ================= */

@Composable
fun TeamColumn(name: String, logo: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(model = logo, contentDescription = null, modifier = Modifier.size(40.dp))
        Text(name)
    }
}

@Composable
fun LineupsPlaceholder() {
    Text("Starting XI coming next 🔜")
}