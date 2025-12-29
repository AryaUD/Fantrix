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
import coil.compose.rememberAsyncImagePainter
import com.example.fantrix.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold() {

    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    val user by remember { mutableStateOf(auth.currentUser) }

    var profileImage by remember { mutableStateOf("") }

    /* ---------------- AUTH GUARD ---------------- */
    LaunchedEffect(user) {
        if (user == null) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    /* -------- REAL-TIME PROFILE IMAGE -------- */
    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            firestore.collection("users")
                .document(uid)
                .addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists()) {
                        profileImage = doc.getString("profileImage") ?: ""
                    }
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

                        IconButton(
                            onClick = {
                                navController.navigate("notifications") {
                                    launchSingleTop = true
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }

                        IconButton(
                            onClick = {
                                navController.navigate("profile") {
                                    launchSingleTop = true
                                }
                            }
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    if (profileImage.isNotEmpty())
                                        profileImage
                                    else
                                        R.drawable.default_avatar
                                ),
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .padding(4.dp)
                            )
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
                        onClick = {
                            navController.navigate("live_matches") {
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.LiveTv, null) },
                        label = { Text("Live") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "watch_party",
                        onClick = {
                            navController.navigate("watch_party") {
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.People, null) },
                        label = { Text("Party") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "home",
                        onClick = {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("Home") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "chat",
                        onClick = {
                            navController.navigate("chat") {
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.Default.Chat, null) },
                        label = { Text("Chat") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "live_feed",
                        onClick = {
                            navController.navigate("live_feed") {
                                launchSingleTop = true
                            }
                        },
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
