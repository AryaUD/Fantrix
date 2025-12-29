package com.example.fantrix.screens

import com.example.fantrix.R
import androidx.compose.foundation.Image
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
    var profileImage by remember { mutableStateOf("") }

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
                }
            }
    }

    Scaffold(containerColor = Color(0xFFF5F6FA)) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(Modifier.height(24.dp))

                    Image(
                        painter = rememberAsyncImagePainter(
                            if (profileImage.isNotBlank())
                                profileImage
                            else
                                R.drawable.default_avatar
                        ),
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(userName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(userEmail, fontSize = 14.sp, color = Color.Gray)

                    IconButton(
                        onClick = { navController.navigate("settings") }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            }

            item {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { navController.navigate("edit_profile") }
                ) {
                    Text("Edit Profile")
                }
            }

            item {
                InfoCard("Favourite Sport") {
                    Text(favouriteSport)
                }
            }

            item {
                InfoCard("About Me") {
                    Text(aboutMe)
                }
            }

            item {
                InfoCard("Member Since") {
                    Text(memberSince)
                }
            }
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
