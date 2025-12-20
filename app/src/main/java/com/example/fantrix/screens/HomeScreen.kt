package com.example.fantrix.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.fantrix.Sports.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun HomeScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    var selectedSport by remember { mutableStateOf("Football") } // ✅ default

    val sports = listOf(
        "Cricket", "Football", "F1", "Kabaddi", "Tennis",
        "Badminton", "Hockey", "Table Tennis", "Baseball",
        "Rugby", "Volleyball", "Golf", "Athletics"
    )

    // ✅ SAFE Firestore call
    LaunchedEffect(Unit) {
        if (user != null) {
            try {
                val snapshot = db.collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                snapshot.getString("preferredSport")?.let {
                    selectedSport = it
                }
            } catch (e: Exception) {
                // fail silently, keep default sport
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {

        LazyRow {
            items(sports.size) { index ->
                FilterChip(
                    selected = sports[index] == selectedSport,
                    onClick = { selectedSport = sports[index] },
                    label = { Text(sports[index]) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ CONTENT ONLY (no Scaffold)
        when (selectedSport) {
            "Cricket" -> CricketScreen()
            "Football" -> FootballScreen(navController)
            "F1" -> F1Screen()
            "Kabaddi" -> KabaddiScreen()
            "Tennis" -> TennisScreen()
            "Badminton" -> BadmintonScreen()
            "Hockey" -> HockeyScreen()
            "Table Tennis" -> TTScreen()
            "Baseball" -> BaseballScreen()
            "Rugby" -> RugbyScreen()
            "Volleyball" -> VolleyballScreen()
            "Golf" -> GolfScreen()
            "Athletics" -> AthleticsScreen()
        }
    }
}
