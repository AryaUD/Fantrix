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
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(user?.uid) {
        if (user == null) return@LaunchedEffect
        try {
            isLoading = true
            val doc = firestore.collection("users").document(user.uid).get().await()
            if (doc.exists()) {
                name = when {
                    doc.contains("fullName") -> doc.getString("fullName") ?: "User"
                    doc.contains("username") -> doc.getString("username") ?: "User"
                    doc.contains("name") -> doc.getString("name") ?: "User"
                    else -> user.displayName ?: "User"
                }
                image = when {
                    doc.contains("profileImage") -> doc.getString("profileImage") ?: ""
                    doc.contains("profileImageUrl") -> doc.getString("profileImageUrl") ?: ""
                    doc.contains("imageUrl") -> doc.getString("imageUrl") ?: ""
                    doc.contains("photoUrl") -> doc.getString("photoUrl") ?: ""
                    else -> ""
                }
                preferredSport = doc.getString("preferredSport") ?: ""
            } else {
                name = user.displayName ?: "User"
                image = user.photoUrl?.toString() ?: ""
            }
        } catch (e: Exception) {
            name = "User"; image = ""; preferredSport = ""
        } finally {
            isLoading = false
        }
    }

    Scaffold { padding ->

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
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                if (image.isNotBlank()) image else R.drawable.default_avatar
                            ),
                            contentDescription = "Profile",
                            modifier = Modifier.size(56.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (preferredSport.isNotBlank()) {
                                Text(
                                    text = preferredSport,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Tap to edit profile",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { navController.navigate("edit_profile") }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            SectionTitle("Account")
            OptionItem(Icons.Default.Person, "Manage Profile") { navController.navigate("edit_profile") }
            OptionItem(Icons.Default.Lock, "Password & Security") { navController.navigate("security") }
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
            OptionItem(Icons.Default.Language, "Language", "English") { showLanguageDialog = true }
            OptionItem(Icons.Default.Info, "About Us")

            Spacer(Modifier.height(16.dp))

            SectionTitle("Support")
            OptionItem(Icons.Default.Help, "Help Center")

            Spacer(Modifier.height(28.dp))

            // ✅ FIXED: Shows confirmation dialog before logout
            Button(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Logout", color = MaterialTheme.colorScheme.onError)
            }
        }
    }

    // ✅ Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        FirebaseAuth.getInstance().signOut()
                        // ✅ FIXED: Navigate to "onboarding" not "login", clear entire back stack
                        navController.navigate("onboarding") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
                                .padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

/* ── UI Parts ── */

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (value != null) {
                Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}