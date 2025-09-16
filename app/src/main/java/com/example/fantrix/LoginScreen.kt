package com.example.fantrix.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome!!! \uD83C\uDFCE\uFE0F ", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please fill in all fields")
                        }
                    } else {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                            .addOnFailureListener {
                                scope.launch {
                                    snackbarHostState.showSnackbar(it.message ?: "Login failed")
                                }
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Login")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { navController.navigate("signup") }) {
                Text("Don't have an account? Sign Up")
            }
        }
    }
}
