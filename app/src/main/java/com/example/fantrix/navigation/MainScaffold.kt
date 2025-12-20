package com.example.fantrix.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold() {

    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    var profileUrl by remember { mutableStateOf<String?>(null) }

    // 🔹 Load profile image once
    LaunchedEffect(user) {
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener {
                    profileUrl = it.getString("profilePictureUrl")
                }
        }
    }

    val hideBars = currentRoute in listOf(
        "splash",
        "login",
        "signup",
        "userDetails",
        "sportsPreference"
    )

    Scaffold(
        topBar = {
            if (!hideBars) {
                TopAppBar(
                    title = { Text("Fantrix") },
                    actions = {

                        // 🔔 Notifications
                        IconButton(onClick = {
                            navController.navigate("notifications")
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }

                        // 👤 Profile
                        IconButton(onClick = {
                            navController.navigate("profile")
                        }) {
                            if (!profileUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = profileUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .padding(4.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Profile"
                                )
                            }
                        }
                    }
                )
            }
        },

        bottomBar = {
            if (!hideBars) {
                NavigationBar {

                    NavigationBarItem(
                        selected = currentRoute == "live_matches",
                        onClick = { navController.navigate("live_matches") },
                        icon = { Icon(Icons.Default.LiveTv, null) },
                        label = { Text("Live") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "watch_party",
                        onClick = { navController.navigate("watch_party") },
                        icon = { Icon(Icons.Default.People, null) },
                        label = { Text("Party") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "home",
                        onClick = { navController.navigate("home") },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("Home") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "chat",
                        onClick = { navController.navigate("chat") },
                        icon = { Icon(Icons.Default.Chat, null) },
                        label = { Text("Chat") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "live_feed",
                        onClick = { navController.navigate("live_feed") },
                        icon = { Icon(Icons.Default.List, null) },
                        label = { Text("Feed") }
                    )
                }
            }
        }
    ) { padding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(padding)
        )
    }
}
