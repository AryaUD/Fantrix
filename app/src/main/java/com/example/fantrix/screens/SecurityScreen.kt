package com.example.fantrix.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SecurityScreen() {

    val user = FirebaseAuth.getInstance().currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("Password & Security", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                user?.email?.let {
                    FirebaseAuth.getInstance().sendPasswordResetEmail(it)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send password reset email")
        }
    }
}
