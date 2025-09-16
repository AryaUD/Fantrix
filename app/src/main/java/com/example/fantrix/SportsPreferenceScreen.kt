package com.example.fantrix.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun SportsPreferenceScreen(navController: NavController) {
    val sports = listOf(
        "Cricket", "Football", "F1", "Kabaddi", "Tennis",
        "Badminton", "Hockey", "Table Tennis", "Baseball",
        "Rugby", "Volleyball", "Golf", "Athletics"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Choose your favorite sport:", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        SportCards(sports, navController)
    }
}

@Composable
fun SportCards(sports: List<String>, navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        sports.forEach { sport ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable {
                        val user = auth.currentUser
                        if (user != null) {
                            db.collection("users").document(user.uid)
                                .update("preferredSport", sport)
                            navController.navigate("home") {
                                popUpTo("sportsPreference") { inclusive = true }
                            }
                        }
                    },
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(sport, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
