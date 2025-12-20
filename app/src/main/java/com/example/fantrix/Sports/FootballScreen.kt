package com.example.fantrix.Sports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.fantrix.Service.FootballServices.Fixture
import com.example.fantrix.Viewmodel.FootballViewModel
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun FootballScreen(
    navController: NavController,
    footballViewModel: FootballViewModel = viewModel()
) {
    val fixtures by footballViewModel.fixtures
    val isLoading by footballViewModel.isLoading

    var selectedLeagueId by remember { mutableStateOf(39) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = {
            footballViewModel.loadFixtures(selectedLeagueId)
        }
    )

    LaunchedEffect(selectedLeagueId) {
        footballViewModel.loadFixtures(selectedLeagueId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            LeagueFilter(
                selectedLeagueId = selectedLeagueId,
                onLeagueSelected = { selectedLeagueId = it }
            )

            Spacer(Modifier.height(12.dp))

            when {
                isLoading -> Text("Loading matches...")
                fixtures.isEmpty() -> Text("No matches available")
                else -> {
                    val grouped = groupFixtures(fixtures)

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        grouped.forEach { (title, matches) ->

                            item {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            items(matches) { match ->
                                MatchCard(
                                    match = match,
                                    onClick = {
                                        match.fixture.id?.let { id ->
                                            navController.navigate("matchDetails/$id")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isLoading,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}



private fun groupFixtures(fixtures: List<Fixture>): Map<String, List<Fixture>> {
    val today = LocalDate.now()

    return fixtures.groupBy { fixture ->
        val matchDate = OffsetDateTime.parse(fixture.fixture.date).toLocalDate()

        when {
            matchDate.isEqual(today.minusDays(1)) ->
                "Yesterday • ${matchDate.format(DateTimeFormatter.ofPattern("dd MMM"))}"
            matchDate.isEqual(today) ->
                "Today • ${matchDate.format(DateTimeFormatter.ofPattern("dd MMM"))}"
            else ->
                matchDate.format(DateTimeFormatter.ofPattern("dd MMM"))
        }
    }
}

@Composable
fun MatchCard(
    match: Fixture,
    onClick: () -> Unit
) {

    val dateTime = OffsetDateTime.parse(match.fixture.date)
        .atZoneSameInstant(ZoneId.systemDefault())
        .toLocalDateTime()

    val dateText = dateTime.format(
        DateTimeFormatter.ofPattern("dd MMMM yyyy")
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(dateText, style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(Modifier.weight(1f)) {
                    AsyncImage(
                        model = match.teams.home.logo,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(match.teams.home.name)
                }

                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "${match.goals.home ?: 0} - ${match.goals.away ?: 0}",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    AsyncImage(
                        model = match.teams.away.logo,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(match.teams.away.name)
                }
            }
        }
    }
}


@Composable
fun LeagueFilter(
    selectedLeagueId: Int,
    onLeagueSelected: (Int) -> Unit
) {
    val leagues = listOf(
        39 to "Premier League",
        140 to "La Liga",
        135 to "Serie A"
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(leagues) { league ->
            FilterChip(
                selected = selectedLeagueId == league.first,
                onClick = { onLeagueSelected(league.first) },
                label = { Text(league.second) }
            )
        }
    }
}
