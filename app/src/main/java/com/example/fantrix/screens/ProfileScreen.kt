package com.example.fantrix.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfileScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser ?: return

    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf(currentUser.email ?: "") }
    var favouriteSport by remember { mutableStateOf("Not set") }
    var aboutMe by remember { mutableStateOf("Tap to add bio") }
    var profilePic by remember { mutableStateOf("") }
    var bannerPic by remember { mutableStateOf("") }

    var showAboutDialog by remember { mutableStateOf(false) }
    var aboutMeEditText by remember { mutableStateOf("") }

    val memberSince = remember {
        currentUser.metadata?.creationTimestamp?.let {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
        } ?: "—"
    }

    LaunchedEffect(currentUser.uid) {
        firestore.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                userName = doc.getString("fullName") ?: ""
                favouriteSport = doc.getString("preferredSport") ?: "Not set"
                aboutMe = doc.getString("about_me") ?: aboutMe
                profilePic = doc.getString("profilePic") ?: ""
                bannerPic = doc.getString("bannerPic") ?: ""
            }
    }

    Scaffold(containerColor = Color(0xFFF5F6FA)) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            item {
                ProfileHeader(
                    userName = userName,
                    userEmail = userEmail,
                    profilePic = profilePic,
                    bannerPic = bannerPic,
                    onSettingsClick = {
                        navController.navigate("settings")
                    }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { navController.navigate("edit_profile") }
                ) {
                    Text("Edit Profile")
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                InfoCard("Favourite Sport") {
                    Text(favouriteSport, fontWeight = FontWeight.Medium)
                }
            }

            item {
                InfoCard("About Me") {
                    Text(
                        aboutMe,
                        modifier = Modifier.clickable {
                            aboutMeEditText = aboutMe
                            showAboutDialog = true
                        }
                    )
                }
            }

            item {
                InfoCard("Member Since") {
                    Text(memberSince)
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    onClick = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Logout")
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Edit About Me") },
            text = {
                TextField(
                    value = aboutMeEditText,
                    onValueChange = { aboutMeEditText = it }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    firestore.collection("users")
                        .document(currentUser.uid)
                        .update("about_me", aboutMeEditText)

                    aboutMe = aboutMeEditText
                    showAboutDialog = false
                }) {
                    Text("Save")
                }
            }
        )
    }
}

/* ---------------- COMPONENTS ---------------- */

@Composable
fun ProfileHeader(
    userName: String,
    userEmail: String,
    profilePic: String,
    bannerPic: String,
    onSettingsClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {

        Image(
            painter = rememberAsyncImagePainter(
                if (bannerPic.isNotEmpty()) bannerPic
                else "https://via.placeholder.com/600x250"
            ),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentScale = ContentScale.Crop
        )

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
        }

        Column(
            modifier = Modifier
                .padding(top = 120.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    if (profilePic.isNotEmpty()) profilePic
                    else "https://via.placeholder.com/150"
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color.White, CircleShape)
            )

            Spacer(Modifier.height(8.dp))

            Text(userName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(userEmail, fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}
