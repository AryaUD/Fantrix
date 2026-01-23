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

@Composable
fun ProfileScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    var name by remember { mutableStateOf("User") }
    var email by remember { mutableStateOf("") }
    var image by remember { mutableStateOf("") }
    var preferredSport by remember { mutableStateOf("") }
    var showLanguageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        user?.uid?.let { uid ->
            firestore.collection("users").document(uid)
                .addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists()) {
                        name = doc.getString("fullName") ?: "User"
                        email = doc.getString("email") ?: user.email.orEmpty()
                        image = doc.getString("profileImage") ?: ""
                        preferredSport = doc.getString("preferredSport") ?: ""
                    }
                }
        }
    }

    Scaffold(containerColor = Color(0xFFF4F6FA)) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()) // ✅ SCROLL FIX
        ) {

            Text(
                text = "Profile",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            /* -------- Profile card -------- */

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
                    Image(
                        painter = rememberAsyncImagePainter(
                            if (image.isNotBlank()) image else R.drawable.default_avatar
                        ),
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (preferredSport.isNotBlank()) {
                            Text(preferredSport, fontSize = 13.sp, color = Color.Gray)
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
