package com.example.fantrix.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun SignupScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            TextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
            Spacer(modifier = Modifier.height(8.dp))
            TextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (email.isBlank() || password.isBlank()) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Please enter both email and password")
                    }
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            val user = result.user
                            if (user != null) {
                                // Create basic user doc
                                val userDoc = hashMapOf(
                                    "email" to email,
                                    "preferredSport" to null
                                )
                                db.collection("users").document(user.uid).set(userDoc)

                                // Navigate to UserDetailsScreen
                                navController.navigate("userDetails") {
                                    popUpTo("signup") { inclusive = true }
                                }
                            }
                        }
                        .addOnFailureListener {
                            scope.launch {
                                snackbarHostState.showSnackbar(it.message ?: "Sign up failed")
                            }
                        }
                }
            }) {
                Text("Sign Up")
            }
        }
    }
}
