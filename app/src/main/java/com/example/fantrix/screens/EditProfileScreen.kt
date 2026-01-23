package com.example.fantrix.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.fantrix.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.InputStream

@Composable
fun EditProfileScreen(navController: NavController) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser ?: return

    var fullName by remember { mutableStateOf("") }
    var aboutMe by remember { mutableStateOf("") }
    var favouriteSport by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var loading by remember { mutableStateOf(false) }

    val sportsList = listOf("Football", "Cricket", "Basketball", "Tennis", "Badminton")

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && isImageUnder1MB(context, uri)) {
            selectedImageUri = uri
        }
    }

    LaunchedEffect(Unit) {
        firestore.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                fullName = doc.getString("fullName") ?: ""
                aboutMe = doc.getString("about_me") ?: ""
                favouriteSport = doc.getString("preferredSport") ?: ""
                profileImageUrl = doc.getString("profileImage")
            }
    }

    Scaffold { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("Edit Profile", fontSize = 22.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(20.dp))

            Image(
                painter = rememberAsyncImagePainter(
                    selectedImageUri ?: profileImageUrl ?: R.drawable.default_avatar
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            )

            TextButton(onClick = { imagePicker.launch("image/*") }) {
                Text("Change profile photo")
            }

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(14.dp))

            Text("Favourite sport", style = MaterialTheme.typography.labelLarge)

            sportsList.forEach { sport ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    onClick = { favouriteSport = sport }
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = favouriteSport == sport,
                            onClick = { favouriteSport = sport }
                        )
                        Text(sport)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = aboutMe,
                onValueChange = { aboutMe = it },
                label = { Text("About me") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(26.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !loading,
                shape = RoundedCornerShape(16.dp),
                onClick = {
                    loading = true

                    scope.launch {
                        try {
                            val imageUrl: String? = selectedImageUri?.let { uri ->
                                withContext(Dispatchers.IO) {
                                    uploadToCloudinary(context, uri)
                                }
                            }

                            val data = hashMapOf<String, Any>(
                                "fullName" to fullName,
                                "preferredSport" to favouriteSport,
                                "about_me" to aboutMe
                            )

                            if (!imageUrl.isNullOrBlank()) {
                                data["profileImage"] = imageUrl
                            }

                            firestore.collection("users")
                                .document(currentUser.uid)
                                .set(data, SetOptions.merge())
                                .addOnSuccessListener {
                                    loading = false
                                    navController.popBackStack()
                                }
                                .addOnFailureListener {
                                    loading = false
                                }

                        } catch (e: Exception) {
                            loading = false
                            e.printStackTrace()
                        }
                    }
                }
            ) {
                Text(if (loading) "Saving..." else "Save changes")
            }
        }
    }
}

/* ---------------- HELPERS (NO ERRORS NOW) ---------------- */

fun isImageUnder1MB(context: Context, uri: Uri): Boolean {
    val size = context.contentResolver
        .openAssetFileDescriptor(uri, "r")
        ?.length ?: 0
    return size in 1..1_000_000
}

fun uploadToCloudinary(context: Context, uri: Uri): String? {

    val cloudName = "dkm3ouqar"
    val uploadPreset = "profile_pics"

    val stream: InputStream =
        context.contentResolver.openInputStream(uri) ?: return null

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart(
            "file",
            "profile.jpg",
            stream.readBytes().toRequestBody("image/*".toMediaTypeOrNull())
        )
        .addFormDataPart("upload_preset", uploadPreset)
        .build()

    val request = Request.Builder()
        .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
        .post(requestBody)
        .build()

    val response = OkHttpClient().newCall(request).execute()
    val json = JSONObject(response.body?.string() ?: return null)
    return json.getString("secure_url")
}
