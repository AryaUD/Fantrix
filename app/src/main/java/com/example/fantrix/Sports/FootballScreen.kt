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

    // ✅ NEW FILTER STATES
    var searchQuery by remember { mutableStateOf("") }
    var footballType by remember { mutableStateOf("Club") } // Club / International
    var category by remember { mutableStateOf("Men") } // Men / Women / U19

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isLoading,
        onRefresh = { footballViewModel.loadFixtures(selectedLeagueId) }
    )

    LaunchedEffect(selectedLeagueId) {
        footballViewModel.loadFixtures(selectedLeagueId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {

        Column(modifier = Modifier.fillMaxSize()) {

            // ✅ TOP FILTER BAR
            FootballTopFilters(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                footballType = footballType,
                onFootballTypeChange = { footballType = it },
                category = category,
                onCategoryChange = { category = it }
            )

            Spacer(Modifier.height(10.dp))

            // ✅ LEAGUE FILTER
            LeagueFilter(
                selectedLeagueId = selectedLeagueId,
                onLeagueSelected = { selectedLeagueId = it },
                searchQuery = searchQuery,
                footballType = footballType,
                category = category
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

/* -------------------- TOP FILTERS -------------------- */

@Composable
fun FootballTopFilters(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    footballType: String,
    onFootballTypeChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search leagues...") },
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FootballTypeDropdown(
                selected = footballType,
                onSelected = onFootballTypeChange
            )

            CategoryFilter(
                selected = category,
                onSelected = onCategoryChange
            )
        }
    }
}

/* -------------------- STABLE DROPDOWN -------------------- */

@Composable
fun FootballTypeDropdown(
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Club", "International")

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.height(35
                .dp)
        ) {
            Text("Type: $selected")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/* -------------------- CATEGORY FILTER -------------------- */

@Composable
fun CategoryFilter(
    selected: String,
    onSelected: (String) -> Unit
) {
    val categories = listOf("Men", "Women", "U19")

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories) { item ->
            FilterChip(
                selected = selected == item,
                onClick = { onSelected(item) },
                label = { Text(item) }
            )
        }
    }
}

/* -------------------- LEAGUE FILTER -------------------- */

@Composable
fun LeagueFilter(
    selectedLeagueId: Int,
    onLeagueSelected: (Int) -> Unit,
    searchQuery: String,
    footballType: String,
    category: String
) {
    val allLeagues = listOf(
        Triple(39, "Premier League", "Club"),
        Triple(140, "La Liga", "Club"),
        Triple(135, "Serie A", "Club"),
        Triple(78, "Bundesliga", "Club"),
        Triple(61, "Ligue 1", "Club"),
        Triple(1, "World Cup", "International"),
        Triple(2, "UEFA Nations League", "International"),
        Triple(4, "Euro Championship", "International")
    )

    val filteredLeagues = allLeagues.filter {
        it.second.contains(searchQuery, ignoreCase = true) &&
                it.third == footballType
        // category ready for backend logic
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(filteredLeagues) { league ->
            FilterChip(
                selected = selectedLeagueId == league.first,
                onClick = { onLeagueSelected(league.first) },
                label = { Text(league.second) }
            )
        }
    }
}

/* -------------------- MATCH UI -------------------- */

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
