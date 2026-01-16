package com.example.fantrix.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailsScreen(navController: NavController) {

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser ?: return

    var fullName by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var preferredSport by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val countryOptions = listOf("India","United States","United Kingdom","Australia","Pakistan","Bangladesh","Sri Lanka","South Africa","New Zealand","Canada","Nepal","UAE","Germany","France","Italy","Singapore","Others")
    val genderOptions = listOf("Male","Female","Other","Prefer not to say")
    val sportOptions = listOf("Cricket","Football","F1","Kabaddi","Tennis","Badminton","Hockey","Table Tennis","Baseball","Rugby","Volleyball","Golf","Athletics")

    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            isUploading = true

            MediaManager.get().upload(uri)
                .option("folder", "fantrix_profiles")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {}
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String?, resultData: Map<*, *>) {
                        profileImageUrl = resultData["secure_url"] as String
                        isUploading = false
                    }
                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        isUploading = false
                    }
                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                }).dispatch()
        }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                )
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(60.dp))

            Text("Complete your profile", fontSize = 28.sp, color = Color.White)
            Text("Fans play better with an identity", fontSize = 14.sp, color = Color.White.copy(0.7f))

            Spacer(Modifier.height(30.dp))

            Box(contentAlignment = Alignment.BottomEnd) {

                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.12f))
                        .clickable { imagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUrl.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(profileImageUrl),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop   // ✅ FIX: perfect circular crop
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(0.7f),
                    modifier = Modifier.padding(6.dp)
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (isUploading) {
                Spacer(Modifier.height(14.dp))
                CircularProgressIndicator(color = Color.White)
            }

            Spacer(Modifier.height(30.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {

                Column(modifier = Modifier.padding(22.dp)) {

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(14.dp))
                    DropdownField("Country", countryOptions, country) { country = it }
                    Spacer(Modifier.height(14.dp))
                    DropdownField("Gender", genderOptions, gender) { gender = it }
                    Spacer(Modifier.height(14.dp))
                    DropdownField("Preferred sport", sportOptions, preferredSport) { preferredSport = it }

                    Spacer(Modifier.height(26.dp))

                    Button(
                        onClick = {
                            isSaving = true

                            val userData = mapOf(
                                "fullName" to fullName,
                                "profileImage" to profileImageUrl,
                                "country" to country,
                                "gender" to gender,
                                "preferredSport" to preferredSport,
                                "profileCompleted" to true
                            )

                            db.collection("users")
                                .document(user.uid)
                                .set(userData)
                                .addOnSuccessListener {
                                    navController.navigate("home") {
                                        popUpTo("userDetails") { inclusive = true }
                                    }
                                }
                                .addOnFailureListener {
                                    isSaving = false
                                }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        enabled = fullName.isNotBlank()
                                && country.isNotBlank()
                                && preferredSport.isNotBlank()
                                && !isUploading
                                && !isSaving,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text("Continue", fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(50.dp))
        }
    }
}

/* ---------------- DROPDOWN ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {

        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onSelect(it)
                        expanded = false
                    }
                )
            }
        }
    }
}
