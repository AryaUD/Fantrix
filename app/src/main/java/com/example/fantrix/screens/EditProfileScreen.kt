package com.example.fantrix.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun EditProfileScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser ?: return

    var fullName by remember { mutableStateOf("") }
    var aboutMe by remember { mutableStateOf("") }
    var favouriteSport by remember { mutableStateOf("") }

    val sportsList = listOf(
        "Football",
        "Cricket",
        "Basketball",
        "Tennis",
        "Badminton"
    )

    // Fetch existing data
    LaunchedEffect(Unit) {
        firestore.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                fullName = doc.getString("fullName") ?: ""
                aboutMe = doc.getString("about_me") ?: ""
                favouriteSport = doc.getString("preferredSport") ?: ""
            }
    }

    Scaffold { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {

            // Title
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(Modifier.height(24.dp))

            // Full Name
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // Favourite Sport
            Text("Favourite Sport")
            Spacer(Modifier.height(8.dp))

            sportsList.forEach { sport ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = favouriteSport == sport,
                        onClick = { favouriteSport = sport }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(sport)
                }
            }

            Spacer(Modifier.height(16.dp))

            // About Me
            OutlinedTextField(
                value = aboutMe,
                onValueChange = { aboutMe = it },
                label = { Text("About Me") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(Modifier.height(32.dp))

            // Save Button
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    firestore.collection("users")
                        .document(currentUser.uid)
                        .update(
                            mapOf(
                                "fullName" to fullName,
                                "preferredSport" to favouriteSport,
                                "about_me" to aboutMe
                            )
                        )
                        .addOnSuccessListener {
                            navController.popBackStack()
                        }
                }
            ) {
                Text("Save Changes")
            }
        }
    }
}
