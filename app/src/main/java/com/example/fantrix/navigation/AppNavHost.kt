package com.example.fantrix.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.fantrix.screens.*

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") { SplashScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("signup") { SignupScreen(navController) }
        composable("userDetails") { UserDetailsScreen(navController) }
        composable("sportsPreference") { SportsPreferenceScreen(navController) }
        composable("home") { HomeScreen(navController) }
    }
}
