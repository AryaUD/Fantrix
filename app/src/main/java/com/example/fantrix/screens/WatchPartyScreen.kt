package com.example.fantrix.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun WatchPartyScreen(navController: NavController) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Watch Party",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // 🎥 Host Party
        Button(
            onClick = { navController.navigate("live_matches_host") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🎥 Host Party")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 🔑 Join Party
        Button(
            onClick = { navController.navigate("join_party") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔑 Join Party")
        }
    }
}