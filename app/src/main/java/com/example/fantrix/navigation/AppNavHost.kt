package com.example.fantrix.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.fantrix.screens.*
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

        composable("live_matches") {
            LiveMatchesScreen()
        }

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

        /* ---------- MATCH DETAILS (FIXED) ---------- */
        composable(
            route = "matchDetails/{fixtureId}",
            arguments = listOf(
                navArgument("fixtureId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val fixtureId =
                backStackEntry.arguments?.getInt("fixtureId")
                    ?: return@composable   // ⛔ prevents crash

            FootballMatchDetailsScreen(
                fixtureId = fixtureId
            )
        }
    }
}
