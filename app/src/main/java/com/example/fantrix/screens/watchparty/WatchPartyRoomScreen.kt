package com.example.fantrix.screens.watchparty

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.fantrix.screens.live.LivePlayerScreen
import com.example.fantrix.webrtc.CameraPreview
import com.example.fantrix.webrtc.WebRTCManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt

@Composable
fun WatchPartyRoomScreen(
    navController: NavController,
    roomId: String
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return

    // Room state
    var matchName by remember { mutableStateOf("") }
    var matchInfo by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var hostId by remember { mutableStateOf("") }
    var participants by remember { mutableStateOf<List<String>>(emptyList()) }

    // Username map: userId -> displayName
    var userNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Camera/mic state
    var isMicOn by remember { mutableStateOf(true) }
    var isCameraOn by remember { mutableStateOf(true) }

    // Floating camera bubble position
    var offsetX by remember { mutableStateOf(16f) }
    var offsetY by remember { mutableStateOf(16f) }
    var screenWidth by remember { mutableStateOf(0f) }
    var screenHeight by remember { mutableStateOf(0f) }
    val bubbleSize = 120.dp
    val density = LocalDensity.current
    val bubblePx = with(density) { bubbleSize.toPx() }

    // WebRTC
    val webRTCManager = remember { WebRTCManager(context) }
    var webRTCReady by remember { mutableStateOf(false) }

    // Permissions
    val cameraGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val audioGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    var permissionsGranted by remember { mutableStateOf(cameraGranted && audioGranted) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results[Manifest.permission.CAMERA] == true &&
                results[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted && !webRTCReady) {
            webRTCManager.initialize()
            webRTCReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { webRTCManager.release() }
    }

    // 🔥 Room listener
    LaunchedEffect(roomId) {
        firestore.collection("watch_parties")
            .document(roomId)
            .addSnapshotListener { document, _ ->
                if (document != null && document.exists()) {
                    matchName = document.getString("matchName") ?: ""
                    matchInfo = document.getString("matchInfo") ?: ""
                    videoUrl = document.getString("videoUrl") ?: ""
                    hostId = document.getString("hostId") ?: ""
                    val participantsMap = document.get("participants") as? Map<*, *>
                    participants = participantsMap?.keys?.map { it.toString() } ?: emptyList()
                } else {
                    navController.popBackStack()
                }
            }
    }

    // ✅ Fetch username for each participant from Firestore
    LaunchedEffect(participants) {
        participants.forEach { uid ->
            if (!userNames.containsKey(uid)) {
                firestore.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        val name = when {
                            doc.contains("fullName") -> doc.getString("fullName") ?: "User"
                            doc.contains("username") -> doc.getString("username") ?: "User"
                            else -> "User"
                        }
                        userNames = userNames + (uid to name)
                    }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                screenWidth = coords.size.width.toFloat()
                screenHeight = coords.size.height.toFloat()
            }
    ) {

        // ── Main content: video + chat ────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = matchName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "🔴 Watch Party  •  ${participants.size} watching",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    // Room code only (no password shown)
                    Text(
                        text = "Room: $roomId",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Video + Chat
            if (videoUrl.isNotEmpty()) {
                LivePlayerScreen(
                    videoUrl = videoUrl,
                    matchName = matchName,
                    matchInfo = matchInfo,
                    matchId = roomId
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ── Floating draggable camera bubbles ─────────────────────────────────
        // Other participants shown as small fixed tiles at bottom-right
        val otherParticipants = participants.filter { it != userId }
        if (otherParticipants.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                otherParticipants.forEach { uid ->
                    OtherParticipantBubble(
                        name = userNames[uid] ?: "...",
                        isHost = uid == hostId
                    )
                }
            }
        }

        // ── MY floating draggable camera bubble ───────────────────────────────
        if (permissionsGranted) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .size(bubbleSize)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        2.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(16.dp)
                    )
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Clamp within screen bounds
                            offsetX = (offsetX + dragAmount.x)
                                .coerceIn(0f, screenWidth - bubblePx)
                            offsetY = (offsetY + dragAmount.y)
                                .coerceIn(0f, screenHeight - bubblePx)
                        }
                    }
            ) {
                // Camera preview
                if (isCameraOn && webRTCReady) {
                    CameraPreview(
                        webRTCManager = webRTCManager,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VideocamOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Username label at bottom
                val myName = userNames[userId] ?: "You"
                Text(
                    text = if (userId == hostId) "👑 $myName" else myName,
                    color = Color.White,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color(0x99000000))
                        .padding(2.dp)
                )

                // ── Mic & Camera toggle buttons on bubble ─────────────────
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Mic toggle
                    SmallControlButton(
                        icon = if (isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
                        isActive = isMicOn,
                        onClick = {
                            isMicOn = !isMicOn
                            webRTCManager.setMicEnabled(isMicOn)
                        }
                    )
                    // Camera toggle
                    SmallControlButton(
                        icon = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        isActive = isCameraOn,
                        onClick = {
                            isCameraOn = !isCameraOn
                            webRTCManager.setCameraEnabled(isCameraOn)
                        }
                    )
                }
            }
        }

        // ── Leave / End Party button (bottom center) ──────────────────────────
        Button(
            onClick = {
                if (userId == hostId) {
                    firestore.collection("watch_parties").document(roomId).delete()
                } else {
                    firestore.collection("watch_parties")
                        .document(roomId)
                        .update("participants.$userId", null)
                }
                webRTCManager.release()
                navController.popBackStack()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .height(44.dp)
        ) {
            Text(
                text = if (userId == hostId) "End Party" else "Leave Party",
                color = MaterialTheme.colorScheme.onError,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Other participant bubble (non-draggable) ──────────────────────────────────
@Composable
fun OtherParticipantBubble(name: String, isHost: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 90.dp, height = 70.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Avatar initial
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.first().uppercase(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Text(
            text = if (isHost) "👑 $name" else name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .padding(2.dp)
        )
    }
}

// ── Small mic/camera button on bubble ────────────────────────────────────────
@Composable
fun SmallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                if (isActive) Color(0x99000000) else MaterialTheme.colorScheme.error
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
    }
}