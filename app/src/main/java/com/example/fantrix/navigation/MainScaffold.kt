package com.example.fantrix.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var user by remember { mutableStateOf(auth.currentUser) }
    var profileImage by remember { mutableStateOf("") }

    // Listen for auth changes
    LaunchedEffect(Unit) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            user = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(authStateListener)
    }

    // Listen for profile image changes
    LaunchedEffect(user?.uid) {
        if (user == null) {
            profileImage = ""
            return@LaunchedEffect
        }

        firestore.collection("users")
            .document(user!!.uid)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {
                    profileImage = doc.getString("profileImage") ?: ""
                }
            }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bars for onboarding and profile setup screens
    val hideBars = currentRoute in listOf(
        "onboarding",
        "userDetails",
        "sportsPreference"
    )

    Scaffold(
        topBar = {
            if (!hideBars && user != null) { // Only show top bar if user is logged in
                TopAppBar(
                    title = { Text("Fantrix") },
                    actions = {

                        IconButton(onClick = {
                            navController.navigate("notifications") { launchSingleTop = true }
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }

                        IconButton(onClick = {
                            navController.navigate("profile") { launchSingleTop = true }
                        }) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = if (profileImage.isNotEmpty())
                                        profileImage
                                    else
                                        R.drawable.default_avatar
                                ),
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                )
            }
        },

        bottomBar = {
            if (!hideBars && user != null) { // Only show bottom bar if user is logged in
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == "live_matches",
                        onClick = { navController.navigate("live_matches") { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.LiveTv, null) },
                        label = { Text("Live") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "watch_party",
                        onClick = { navController.navigate("watch_party") { launchSingleTop = true } },
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
                        selected = currentRoute == "Arcade",
                        onClick = { navController.navigate("Arcade") { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.ConnectingAirports, null) },
                        label = { Text("Arcade") }
                    )

                    NavigationBarItem(
                        selected = currentRoute == "live_feed",
                        onClick = { navController.navigate("live_feed") { launchSingleTop = true } },
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