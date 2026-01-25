package com.example.fantrix.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.fantrix.R
import com.example.fantrix.theme.ThemeManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    var name by remember { mutableStateOf("User") }
    var image by remember { mutableStateOf("") }
    var preferredSport by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Load user data
    LaunchedEffect(user?.uid) {
        if (user == null) return@LaunchedEffect

        try {
            isLoading = true

            // Try multiple field names for username
            val doc = firestore.collection("users").document(user.uid).get().await()

            if (doc.exists()) {
                // Check different possible field names for name
                name = when {
                    doc.contains("fullName") -> doc.getString("fullName") ?: "User"
                    doc.contains("username") -> doc.getString("username") ?: "User"
                    doc.contains("name") -> doc.getString("name") ?: "User"
                    else -> user.displayName ?: "User"
                }

                // Check different possible field names for profile image
                image = when {
                    doc.contains("profileImage") -> doc.getString("profileImage") ?: ""
                    doc.contains("profileImageUrl") -> doc.getString("profileImageUrl") ?: ""
                    doc.contains("imageUrl") -> doc.getString("imageUrl") ?: ""
                    doc.contains("photoUrl") -> doc.getString("photoUrl") ?: ""
                    else -> ""
                }

                preferredSport = doc.getString("preferredSport") ?: ""
            } else {
                // If no document exists, use Firebase Auth data
                name = user.displayName ?: "User"
                image = user.photoUrl?.toString() ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            name = "User"
            image = ""
            preferredSport = ""
        } finally {
            isLoading = false
        }
    }

    Scaffold(containerColor = Color(0xFFF4F6FA)) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            Text(
                text = "Profile",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            /* -------- Profile card -------- */

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Image
                        Image(
                            painter = rememberAsyncImagePainter(
                                if (image.isNotBlank()) {
                                    image
                                } else {
                                    R.drawable.default_avatar
                                }
                            ),
                            contentDescription = "Profile",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(Modifier.width(12.dp))

                        Column {
                            Text(
                                text = name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            if (preferredSport.isNotBlank()) {
                                Text(
                                    text = preferredSport,
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = "Tap to edit profile",
                                fontSize = 12.sp,
                                color = Color(0xFF2196F3),
                                modifier = Modifier.clickable {
                                    navController.navigate("edit_profile")
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            SectionTitle("Account")
            OptionItem(Icons.Default.Person, "Manage Profile") {
                navController.navigate("edit_profile")
            }
            OptionItem(Icons.Default.Lock, "Password & Security") {
                navController.navigate("security")
            }
            OptionItem(Icons.Default.Notifications, "Notifications")

            Spacer(Modifier.height(12.dp))

            SectionTitle("Preferences")
            OptionItem(
                Icons.Default.DarkMode,
                "Theme",
                if (ThemeManager.isDarkTheme.value) "Dark" else "Light"
            ) {
                ThemeManager.isDarkTheme.value = !ThemeManager.isDarkTheme.value
            }

            OptionItem(Icons.Default.Language, "Language", "English") {
                showLanguageDialog = true
            }

            OptionItem(Icons.Default.Info, "About Us")

            Spacer(Modifier.height(16.dp))

            SectionTitle("Support")
            OptionItem(Icons.Default.Help, "Help Center")

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Logout", color = Color.White)
            }
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Choose Language") },
            text = {
                Column {
                    listOf("English", "Hindi", "Spanish").forEach {
                        Text(
                            text = it,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLanguageDialog = false }
                                .padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

/* ----------------- UI PARTS ----------------- */

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = Color.Gray,
        modifier = Modifier.padding(start = 20.dp, bottom = 6.dp, top = 10.dp)
    )
}

@Composable
fun OptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String? = null,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title)
            Spacer(Modifier.width(12.dp))
            Text(title, modifier = Modifier.weight(1f), fontSize = 15.sp)
            if (value != null) {
                Text(value, fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.width(6.dp))
            }
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}