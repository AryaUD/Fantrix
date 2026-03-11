package com.example.fantrix.screens.watchparty

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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

    // Generate a strong 10-character alphanumeric room code once
    val roomId = remember {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        (1..10).map { chars.random() }.joinToString("")
    }

    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var roomCreated by remember { mutableStateOf(false) }
    var showCopiedToast by remember { mutableStateOf(false) }

    val inviteText = "Join my Watch Party on Fantrix!\n" +
            "Match: $matchName\n" +
            "Room Code: $roomId\n" +
            "Password: $password\n" +
            "Open Fantrix → Watch Party → Join Party"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Header ───────────────────────────────────────────────────────
            Text(
                text = "Host Watch Party",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            // ── Match Card ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF16213E))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Selected Match",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = matchName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = matchInfo,
                        color = Color(0xFF5865F2),
                        fontSize = 13.sp
                    )
                }
            }

            // ── Room Code Display ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF16213E))
                    .border(1.dp, Color(0xFF5865F2), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Room Code",
                        color = Color(0xFFAAAAAA),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = roomId,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                        as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText("Room Code", roomId)
                                )
                                showCopiedToast = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Code",
                                tint = Color(0xFF5865F2)
                            )
                        }
                    }
                    Text(
                        text = "Share this code with friends to join",
                        color = Color(0xFFAAAAAA),
                        fontSize = 11.sp
                    )
                }
            }

            if (showCopiedToast) {
                Text(
                    text = "✅ Room code copied!",
                    color = Color(0xFF43A047),
                    fontSize = 13.sp
                )
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showCopiedToast = false
                }
            }

            // ── Password Field ────────────────────────────────────────────────
            Column {
                Text(
                    text = "Set Party Password",
                    color = Color(0xFFAAAAAA),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = ""
                    },
                    placeholder = { Text("Enter a password", color = Color(0xFF666666)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF5865F2)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Default.VisibilityOff
                                else
                                    Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color(0xFFAAAAAA)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF5865F2),
                        unfocusedBorderColor = Color(0xFF333355),
                        cursorColor = Color(0xFF5865F2)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    isError = passwordError.isNotEmpty()
                )
                if (passwordError.isNotEmpty()) {
                    Text(
                        text = passwordError,
                        color = Color(0xFFE53935),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Share Invite Button ───────────────────────────────────────────
            OutlinedButton(
                onClick = {
                    if (password.length < 4) {
                        passwordError = "Set a password first (min 4 characters)"
                        return@OutlinedButton
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, inviteText)
                    }
                    context.startActivity(
                        Intent.createChooser(shareIntent, "Share Watch Party Invite")
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5865F2)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5865F2))
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Invite (WhatsApp, SMS...)", fontWeight = FontWeight.SemiBold)
            }

            // ── Start Party Button ────────────────────────────────────────────
            Button(
                onClick = {
                    if (password.length < 4) {
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
                        "password" to password,
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
                                // Remove setup screen from back stack
                                popUpTo("host_setup/$matchId") { inclusive = true }
                            }
                        }
                        .addOnFailureListener {
                            isLoading = false
                            passwordError = "Failed to create room. Try again."
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5865F2)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "🎉 Start Watch Party",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}