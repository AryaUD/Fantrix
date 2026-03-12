package com.example.fantrix.screens.watchparty

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.fantrix.screens.live.LivePlayerScreen
import com.example.fantrix.webrtc.CameraPreview
import com.example.fantrix.webrtc.WebRTCManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun WatchPartyRoomScreen(
    navController: NavController,
    roomId: String
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userId = auth.currentUser?.uid ?: return

    var matchName by remember { mutableStateOf("") }
    var matchInfo by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var hostId by remember { mutableStateOf("") }
    var participants by remember { mutableStateOf<List<String>>(emptyList()) }

    var isMicOn by remember { mutableStateOf(true) }
    var isCameraOn by remember { mutableStateOf(true) }

    val webRTCManager = remember { WebRTCManager(context) }
    var webRTCReady by remember { mutableStateOf(false) }

    val cameraGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val audioGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    var permissionsGranted by remember { mutableStateOf(cameraGranted && audioGranted) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
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

    LaunchedEffect(roomId) {
        firestore.collection("watch_parties")
            .document(roomId)
            .addSnapshotListener { document, _ ->
                if (document != null && document.exists()) {
                    matchName = document.getString("matchName") ?: ""
                    matchInfo = document.getString("matchInfo") ?: ""
                    videoUrl = document.getString("videoUrl") ?: ""
                    password = document.getString("password") ?: ""
                    hostId = document.getString("hostId") ?: ""
                    val participantsMap = document.get("participants") as? Map<*, *>
                    participants = participantsMap?.keys?.map { it.toString() } ?: emptyList()
                } else {
                    navController.popBackStack()
                }
            }
    }

    // ── Use MaterialTheme colors throughout ──────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // ── Top Bar ──────────────────────────────────────────────────────────
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
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Room: $roomId",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Pass: $password",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // ── Match Stream ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
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
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // ── Participant Tiles ─────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(participants) { participantId ->
                    ParticipantTile(
                        participantId = participantId,
                        isCurrentUser = participantId == userId,
                        isHost = participantId == hostId,
                        webRTCManager = if (participantId == userId) webRTCManager else null,
                        showVideo = participantId == userId && isCameraOn && webRTCReady
                    )
                }
            }
        }

        // ── Controls Bar ─────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(
                    icon = if (isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
                    label = if (isMicOn) "Mute" else "Unmute",
                    isActive = isMicOn,
                    onClick = {
                        isMicOn = !isMicOn
                        webRTCManager.setMicEnabled(isMicOn)
                    }
                )

                ControlButton(
                    icon = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    label = if (isCameraOn) "Stop Video" else "Start Video",
                    isActive = isCameraOn,
                    onClick = {
                        isCameraOn = !isCameraOn
                        webRTCManager.setCameraEnabled(isCameraOn)
                    }
                )

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
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (userId == hostId) "End Party" else "Leave",
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Participant tile ──────────────────────────────────────────────────────────
@Composable
fun ParticipantTile(
    participantId: String,
    isCurrentUser: Boolean,
    isHost: Boolean,
    webRTCManager: WebRTCManager?,
    showVideo: Boolean
) {
    val shortName = if (isCurrentUser) "You" else participantId.take(6)

    Box(
        modifier = Modifier
            .size(width = 100.dp, height = 80.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (isCurrentUser) 2.dp else 1.dp,
                color = if (isCurrentUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (showVideo && webRTCManager != null) {
            CameraPreview(
                webRTCManager = webRTCManager,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = shortName.first().uppercase(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isHost) "👑 $shortName" else shortName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Control button ────────────────────────────────────────────────────────────
@Composable
fun ControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isActive)
                        MaterialTheme.colorScheme.secondaryContainer
                    else
                        MaterialTheme.colorScheme.error
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
    }
}