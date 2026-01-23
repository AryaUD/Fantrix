package com.example.fantrix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.fantrix.navigation.MainScaffold
import com.example.fantrix.theme.ThemeManager
import com.example.fantrix.ui.theme.FantrixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val isDark = ThemeManager.isDarkTheme.value

            FantrixTheme(darkTheme = isDark) {
                MainScaffold()
            }
        }
    }
}
