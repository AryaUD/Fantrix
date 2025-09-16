package com.example.fantrix.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun UserDetailsScreen(navController: NavController) {
    var fullName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val user = auth.currentUser

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Function to show DatePickerDialog
    fun showDatePicker() {
        val activity = context as? android.app.Activity
        activity?.let {
            val picker = DatePickerDialog(
                it,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    dob = format.format(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            picker.datePicker.maxDate = System.currentTimeMillis()
            picker.show()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> Snackbar(snackbarData = data) } }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // DOB field wrapped in Box for reliable click
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dob,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date of Birth") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier
                    .matchParentSize()
                    .clickable{ showDatePicker() })
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = gender,
                onValueChange = { gender = it },
                label = { Text("Gender (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = country,
                onValueChange = { country = it },
                label = { Text("Country") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (fullName.isBlank() || dob.isBlank() || country.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please fill in all required fields")
                        }
                        return@Button
                    }

                    if (user != null) {
                        val userDetails = mapOf(
                            "fullName" to fullName,
                            "dob" to dob,
                            "gender" to gender.ifEmpty { null },
                            "country" to country
                        )

                        db.collection("users").document(user.uid).update(userDetails)
                            .addOnSuccessListener {
                                navController.navigate("sportsPreference") {
                                    popUpTo("userDetails") { inclusive = true }
                                }
                            }
                            .addOnFailureListener {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Failed to save details")
                                }
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save & Continue")
            }
        }
    }
}
