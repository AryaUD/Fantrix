package com.example.fantrix.screens.watchparty

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun JoinPartyScreen(navController: NavController) {

    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return

    var roomId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Join Watch Party",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = roomId,
            onValueChange = { roomId = it.trim() },
            label = { Text("Room Code") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it.trim() },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {

                if (roomId.isEmpty() || password.isEmpty()) {
                    errorMessage = "Please enter room code and password"
                    return@Button
                }

                isLoading = true
                errorMessage = ""

                firestore.collection("watch_parties")
                    .document(roomId)
                    .get()
                    .addOnSuccessListener { document ->

                        if (document.exists() &&
                            document.getString("password") == password &&
                            document.getBoolean("isLive") == true
                        ) {

                            // Add participant
                            firestore.collection("watch_parties")
                                .document(roomId)
                                .update("participants.$userId", true)

                            navController.navigate("party_room/$roomId")

                        } else {
                            errorMessage = "Invalid room code or password"
                        }

                        isLoading = false
                    }
                    .addOnFailureListener {
                        errorMessage = "Something went wrong"
                        isLoading = false
                    }

            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Join Party")
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}