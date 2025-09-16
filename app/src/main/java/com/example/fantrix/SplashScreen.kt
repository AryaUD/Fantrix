package com.example.fantrix.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.fantrix.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val galada = FontFamily(Font(R.font.galada_regular))

    var animatedIn by remember { mutableStateOf(false) }
    val scale = animateFloatAsState(
        targetValue = if (animatedIn) 1.0f else 0.85f,
        animationSpec = tween(durationMillis = 700)
    )

    LaunchedEffect(Unit) {
        animatedIn = true
        delay(2000) // show splash

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // Already signed in → go to HomeScreen
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            // Not signed in → go to LoginScreen
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Fantrix",
            fontFamily = galada,
            fontSize = 48.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.scale(scale.value)
        )
    }
}
