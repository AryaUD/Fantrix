package com.example.fantrix.Sports.MatchDetails

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.fantrix.Service.FootballEvent

@Composable
fun MatchTimeline(events: List<FootballEvent>) {

    if (events.isEmpty()) {
        Text("No events available")
        return
    }

    Column {
        Text(
            text = "TIMELINE",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        events.forEach { event ->

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                // ⏱ TIME
                Text("${event.time.elapsed ?: "-"}'")

                // ⚽ EVENT
                Column(Modifier.weight(1f)) {
                    Text(event.player.name ?: "")

                    Text(
                        text = when (event.type) {
                            "Goal" -> "⚽ ${event.detail}"
                            "Card" -> if (event.detail.contains("Red")) "🟥 Red Card" else "🟨 Yellow Card"
                            "subst" -> "🔄 Substitution"
                            else -> event.type
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // 🏟 TEAM LOGO
                AsyncImage(
                    model = event.team.logo,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
