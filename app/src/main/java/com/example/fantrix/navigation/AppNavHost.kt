package com.example.fantrix.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.fantrix.screens.*
import com.example.fantrix.screens.live.LiveMatchesScreen
import com.example.fantrix.screens.live.LivePlayerScreen
import com.example.fantrix.Sports.FootballMatchDetails.FootballMatchDetailsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier
    ) {

        /* ---------- AUTH FLOW ---------- */
        composable("splash") {
            SplashScreen(navController)
        }

        composable("login") {
            LoginScreen(navController)
        }

        composable("signup") {
            SignupScreen(navController)
        }

        composable("userDetails") {
            UserDetailsScreen(navController)
        }

        composable("sportsPreference") {
            SportsPreferenceScreen(navController)
        }

        /* ---------- MAIN SCREENS ---------- */
        composable("home") {
            HomeScreen(navController)
        }

        composable("profile") {
            ProfileScreen(navController)
        }

        composable("edit_profile") {
            EditProfileScreen(navController)
        }

        composable("settings") {
            SettingsScreen(navController)
        }

        /* ---------- LIVE MATCH FLOW ---------- */
        composable("live_matches") {
            LiveMatchesScreen(navController)
        }

        composable("live_player") {
            LivePlayerScreen()
        }

        /* ---------- OTHER SCREENS ---------- */
        composable("watch_party") {
            WatchPartyScreen()
        }

        composable("chat") {
            ChatScreen()
        }

        composable("live_feed") {
            LiveFeedScreen()
        }

        composable("notifications") {
            NotificationsScreen()
        }

        /* ---------- MATCH DETAILS ---------- */
        composable(
            route = "matchDetails/{fixtureId}",
            arguments = listOf(
                navArgument("fixtureId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val fixtureId =
                backStackEntry.arguments?.getInt("fixtureId") ?: return@composable

            FootballMatchDetailsScreen(
                fixtureId = fixtureId
            )
        }
    }
}
