package com.example.fantrix.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.fantrix.screens.*
import com.example.fantrix.screens.live.LivePlayerScreen
import com.example.fantrix.Sports.FootballMatchDetails.FootballMatchDetailsScreen

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

        composable("live_matches") { LiveMatchesScreen(navController) }

        // ✅ Football match details screen
        composable(
            route = "matchDetails/{fixtureId}",
            arguments = listOf(navArgument("fixtureId") { type = NavType.IntType })
        ) { backStackEntry ->
            val fixtureId = backStackEntry.arguments?.getInt("fixtureId") ?: 0

            FootballMatchDetailsScreen(
                fixtureId = fixtureId,
                navController = navController
            )
        }

        // ✅ Live player
        composable(
            route = "live_player?url={url}&name={name}&info={info}&id={id}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
                navArgument("info") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val info = backStackEntry.arguments?.getString("info") ?: ""
            val id = backStackEntry.arguments?.getString("id") ?: ""

            LivePlayerScreen(
                videoUrl = url,
                matchName = name,
                matchInfo = info,
                matchId = id
            )
        }

        composable("watch_party") { WatchPartyScreen() }
        composable("Arcade") { ArcadeScreen() }
        composable("live_feed") { LiveFeedScreen() }
        composable("notifications") { NotificationsScreen() }
        composable("security") { SecurityScreen() }
    }
}