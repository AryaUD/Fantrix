package com.example.fantrix.screens.watchparty

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun HostPartySetupScreen(
    navController: NavController,
    matchId: String,
    matchName: String,
    matchInfo: String,
    videoUrl: String
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return

    val roomId = remember {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        (1..10).map { chars.random() }.joinToString("")
    }

    // ✅ Public/Private toggle
    var isPublic by remember { mutableStateOf(true) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showCopied by remember { mutableStateOf(false) }

    val inviteText = if (isPublic) {
        "Join my Watch Party on Fantrix!\n" +
                "Match: $matchName\n" +
                "Room Code: $roomId\n" +
                "This is a PUBLIC room — open Fantrix and find it in Watch Party!"
    } else {
        "Join my Watch Party on Fantrix!\n" +
                "Match: $matchName\n" +
                "Room Code: $roomId\n" +
                "Password: $password\n" +
                "Open Fantrix → Watch Party → Join Party"
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Header ───────────────────────────────────────────────────────
            Text(
                text = "Host Watch Party",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // ── Selected Match Card ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Selected Match",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = matchName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = matchInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Room Code ────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Room Code",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = roomId,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Room Code", roomId))
                            showCopied = true
                        }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    if (showCopied) {
                        Text(
                            text = "Copied!",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall
                        )
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(2000)
                            showCopied = false
                        }
                    }
                }
            }

            // ── Public / Private Toggle ───────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isPublic) "Public Room" else "Private Room",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = if (isPublic)
                                "Anyone can see and join this room"
                            else
                                "Only people with the password can join",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPublic) Icons.Default.Public else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isPublic) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = isPublic,
                            onCheckedChange = {
                                isPublic = it
                                if (it) password = ""
                                passwordError = ""
                            }
                        )
                    }
                }
            }

            // ── Password Field (only for private) ─────────────────────────────
            if (!isPublic) {
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = ""
                    },
                    label = { Text("Set Party Password") },
                    placeholder = { Text("Min 4 characters") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = passwordError.isNotEmpty(),
                    supportingText = {
                        if (passwordError.isNotEmpty())
                            Text(passwordError, color = MaterialTheme.colorScheme.error)
                    }
                )
            }

            Spacer(Modifier.weight(1f))

            // ── Share Button ─────────────────────────────────────────────────
            OutlinedButton(
                onClick = {
                    if (!isPublic && password.length < 4) {
                        passwordError = "Set a password first (min 4 characters)"
                        return@OutlinedButton
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, inviteText)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Invite"))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share Invite", fontWeight = FontWeight.SemiBold)
            }

            // ── Start Party Button ────────────────────────────────────────────
            Button(
                onClick = {
                    if (!isPublic && password.length < 4) {
                        passwordError = "Password must be at least 4 characters"
                        return@Button
                    }
                    isLoading = true
                    val roomData = hashMapOf(
                        "hostId" to userId,
                        "matchId" to matchId,
                        "matchName" to matchName,
                        "matchInfo" to matchInfo,
                        "videoUrl" to videoUrl,
                        "password" to if (isPublic) "" else password,
                        "isPublic" to isPublic,   // ✅ Store public/private flag
                        "isLive" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "participants" to hashMapOf(userId to true)
                    )
                    firestore.collection("watch_parties")
                        .document(roomId)
                        .set(roomData)
                        .addOnSuccessListener {
                            isLoading = false
                            navController.navigate("party_room/$roomId") {
                                popUpTo("host_setup/$matchId") { inclusive = true }
                            }
                        }
                        .addOnFailureListener {
                            isLoading = false
                            passwordError = "Failed to create room. Try again."
                        }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Start Watch Party",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}