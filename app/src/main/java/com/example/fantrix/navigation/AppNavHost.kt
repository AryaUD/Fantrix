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
import com.example.fantrix.screens.watchparty.HostPartySetupScreen
import com.example.fantrix.screens.watchparty.JoinPartyScreen
import com.example.fantrix.screens.watchparty.WatchPartyRoomScreen
import com.example.fantrix.screens.watchparty.WatchPartyScreen

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

        // ✅ Normal Live Matches (unchanged - watch without any room)
        composable("live_matches") {
            LiveMatchesScreen(navController)
        }

        // ✅ Host Mode - select match to host
        composable("live_matches_host") {
            LiveMatchesScreen(
                navController = navController,
                isHostMode = true
            )
        }

        // ✅ Host Party Setup - set password, share room code
        composable(
            route = "host_setup/{matchId}?matchName={matchName}&matchInfo={matchInfo}&videoUrl={videoUrl}",
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("matchName") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("matchInfo") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("videoUrl") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            HostPartySetupScreen(
                navController = navController,
                matchId = backStackEntry.arguments?.getString("matchId") ?: "",
                matchName = backStackEntry.arguments?.getString("matchName") ?: "",
                matchInfo = backStackEntry.arguments?.getString("matchInfo") ?: "",
                videoUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
            )
        }

        // ✅ Watch Party Main Screen
        composable("watch_party") {
            WatchPartyScreen(navController)
        }

        // ✅ Join Party
        composable("join_party") {
            JoinPartyScreen(navController)
        }

        // ✅ Party Room
        composable(
            route = "party_room/{roomId}",
            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            WatchPartyRoomScreen(navController, roomId)
        }

        // ✅ Football match details
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

        // ✅ Live Player (normal watch - completely separate from watch party)
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

        composable("Arcade") { ArcadeScreen() }
        composable("live_feed") { LiveFeedScreen() }
        composable("notifications") { NotificationsScreen() }
        composable("security") { SecurityScreen() }
    }
}