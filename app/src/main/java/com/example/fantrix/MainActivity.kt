package com.example.fantrix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.fantrix.navigation.AppNavHost
import com.example.fantrix.ui.theme.FantrixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FantrixTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}
