package com.example.fantrix.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class Match(
    val sport: String,
    val teamA: String,
    val teamB: String,
    val title: String
)

@Composable
fun HomeScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    var sport by remember { mutableStateOf<String?>(null) }
    var selectedSport by remember { mutableStateOf<String?>(null) }
    var userName by remember { mutableStateOf<String?>(null) }
    var profileUrl by remember { mutableStateOf<String?>(null) }

    val sports = listOf(
        "Cricket",
        "Football",
        "F1",
        "Kabaddi",
        "Tennis",
        "Badminton",
        "Hockey",
        "Table Tennis",
        "Baseball",
        "Rugby",
        "Volleyball",
        "Golf",
        "Athletics"
    )

    val allLiveMatches = listOf(
        Match("Cricket", "India 🇮🇳", "Australia 🇦🇺", "Playoff Match"),
        Match("Football", "Manchester United", "Chelsea", "League Match"),
        Match("F1", "Hamilton", "Verstappen", "Grand Prix"),
        Match("Kabaddi", "Team A", "Team B", "Semi Final"),
        Match("Tennis", "Federer", "Nadal", "Final Match")
    )

    LaunchedEffect(user) {
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    sport = document.getString("preferredSport")
                    selectedSport = sport
                    userName = document.getString("fullName")
                    profileUrl = document.getString("profilePictureUrl")
                }
        }
    }

    val filteredMatches = selectedSport?.let { s ->
        allLiveMatches.filter { it.sport == s }
    } ?: allLiveMatches

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hello, ${userName ?: "User"}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Welcome back 👋",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            IconButton(onClick = { /* Navigate to profile */ }) {
                if (!profileUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = profileUrl,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sports Chips
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

        Spacer(modifier = Modifier.height(24.dp))

        // Now on Air
        Text(
            text = "Now on Air",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(12.dp))

        filteredMatches.forEach { match ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(match.title, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(match.teamA, style = MaterialTheme.typography.bodyLarge)
                        Text(match.teamB, style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { /* join match */ },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Join")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Live Scorecards
        Text(
            text = "Live Scorecards",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Card(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("India 248/5")
                    Text("Australia 419/2")
                }
            }
            Card(
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("SA 192/6")
                    Text("England 258/7")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Chat with Friends
        Text(
            text = "Chat with Friends",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Friend",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Michael", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logout Button
        Button(
            onClick = {
                auth.signOut()
                navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Logout")
        }
    }
}
