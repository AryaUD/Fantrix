package com.example.fantrix.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.fantrix.screens.*
import com.example.fantrix.screens.LiveMatchesScreen
import com.example.fantrix.screens.live.LivePlayerScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "onboarding",
        modifier = modifier
    ) {

        composable("onboarding") { OnboardingFlowScreen(navController) }
        composable("userDetails") { UserDetailsScreen(navController) }
        composable("sportsPreference") { SportsPreferenceScreen(navController) }
        composable("home") { HomeScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
        composable("edit_profile") { EditProfileScreen(navController) }
        composable("settings") { SettingsScreen(navController) }

        // ✅ Live matches
        composable("live_matches")
        { LiveMatchesScreen(navController) }
        composable("live_player") {
            LivePlayerScreen()
        }

        composable("watch_party") { WatchPartyScreen() }
        composable("Arcade") { ArcadeScreen() }
        composable("live_feed") { LiveFeedScreen() }
        composable("notifications") { NotificationsScreen() }
        composable("security") { SecurityScreen() }
    }
}
