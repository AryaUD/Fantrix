package com.example.fantrix.screens

import com.example.fantrix.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
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

data class Badge(
    val name: String = "",
    val iconUrl: String = "",
    val description: String = ""
)

data class ArcadeActivity(
    val type: String = "",
    val result: String = "",
    val date: String = ""
)

@Composable
fun ProfileScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser ?: return

    var userName by remember { mutableStateOf("") }
    var favouriteSport by remember { mutableStateOf("Not set") }
    var aboutMe by remember { mutableStateOf("Tap to add bio") }
    var profileImage by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var badges by remember { mutableStateOf<List<Badge>>(emptyList()) }
    var arcadeActivity by remember { mutableStateOf<List<ArcadeActivity>>(emptyList()) }

    val memberSince = remember {
        currentUser.metadata?.creationTimestamp?.let {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
        } ?: "—"
    }

    LaunchedEffect(currentUser.uid) {
        firestore.collection("users")
            .document(currentUser.uid)
            .addSnapshotListener { doc, _ ->
                if (doc != null && doc.exists()) {

                    userName = doc.getString("fullName") ?: ""
                    favouriteSport = doc.getString("preferredSport") ?: "Not set"
                    aboutMe = doc.getString("about_me") ?: aboutMe
                    profileImage = doc.getString("profileImage") ?: ""
                    country = doc.getString("country") ?: ""

                    val fetchedBadges = doc.get("badges") as? List<Map<String, Any>> ?: emptyList()
                    badges = fetchedBadges.map {
                        Badge(
                            name = it["name"] as? String ?: "",
                            iconUrl = it["iconUrl"] as? String ?: "",
                            description = it["description"] as? String ?: ""
                        )
                    }

                    val fetchedArcade = doc.get("arcadeActivity") as? List<Map<String, Any>> ?: emptyList()
                    arcadeActivity = fetchedArcade.map {
                        ArcadeActivity(
                            type = it["type"] as? String ?: "",
                            result = it["result"] as? String ?: "",
                            date = it["date"] as? String ?: ""
                        )
                    }
                }
            }
    }

    Scaffold(containerColor = Color(0xFFF4F6FA)) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            /* ---------- TOP PROFILE HEADER ---------- */

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF0F2027), Color(0xFF203A43))
                            )
                        )
                        .padding(bottom = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Spacer(Modifier.height(30.dp))

                        Image(
                            painter = rememberAsyncImagePainter(
                                if (profileImage.isNotBlank()) profileImage else R.drawable.default_avatar
                            ),
                            contentDescription = "Profile",
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop   // ✅ FIXED: perfect circle fill
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(userName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)

                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    }
                }
            }

            /* ---------- EDIT PROFILE ---------- */

            item {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    onClick = { navController.navigate("edit_profile") }
                ) {
                    Text("Edit Profile")
                }
            }

            /* ---------- BADGES ---------- */

            if (badges.isNotEmpty()) {
                item {
                    InfoCard("Fan Badges") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            badges.forEach { badge ->
                                Image(
                                    painter = rememberAsyncImagePainter(badge.iconUrl),
                                    contentDescription = badge.name,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }
                    }
                }
            }

            /* ---------- ARCADE ---------- */

            if (arcadeActivity.isNotEmpty()) {
                item {
                    InfoCard("Arcade Activity") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            arcadeActivity.take(3).forEach {
                                Text("• ${it.type} – ${it.result}", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            item { InfoCard("Favourite Sport") { Text(favouriteSport) } }
            item { InfoCard("About Me") { Text(aboutMe) } }

            if (country.isNotBlank()) {
                item { InfoCard("Country") { Text(country) } }
            }

            item { InfoCard("Member Since") { Text(memberSince) } }

            item { Spacer(Modifier.height(30.dp)) }
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}
