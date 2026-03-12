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
import androidx.compose.material.icons.filled.*
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
import com.example.fantrix.webrtc.CameraPreview
import com.example.fantrix.webrtc.WebRTCManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import org.webrtc.VideoTrack
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
    var prevParticipants by remember { mutableStateOf<List<String>>(emptyList()) }
    var userNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Room ended state
    var roomEnded by remember { mutableStateOf(false) }
    var redirectCountdown by remember { mutableStateOf(5) }

    // Controls
    var isMicOn by remember { mutableStateOf(true) }
    var isCameraOn by remember { mutableStateOf(true) }

    // Draggable bubble
    var offsetX by remember { mutableStateOf(16f) }
    var offsetY by remember { mutableStateOf(16f) }
    var screenWidth by remember { mutableStateOf(0f) }
    var screenHeight by remember { mutableStateOf(0f) }
    val bubbleSize = 120.dp
    val density = LocalDensity.current
    val bubblePx = with(density) { bubbleSize.toPx() }

    // Remote video tracks from WebRTC
    var remoteVideoTracks by remember { mutableStateOf<Map<String, VideoTrack>>(emptyMap()) }

    // WebRTC
    val webRTCManager = remember { WebRTCManager(context) }
    var webRTCReady by remember { mutableStateOf(false) }
    var webRTCJoined by remember { mutableStateOf(false) }

    // Permissions
    val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val audioGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    var permissionsGranted by remember { mutableStateOf(cameraGranted && audioGranted) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results[Manifest.permission.CAMERA] == true &&
                results[Manifest.permission.RECORD_AUDIO] == true
    }

    // ── Setup WebRTC callbacks ─────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        webRTCManager.onRemoteTrackAdded = { peerId, track ->
            remoteVideoTracks = remoteVideoTracks + (peerId to track)
        }
        webRTCManager.onRemoteTrackRemoved = { peerId ->
            remoteVideoTracks = remoteVideoTracks - peerId
        }
    }

    // ── Request permissions ────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    // ── Initialize WebRTC once permissions granted ─────────────────────────────
    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted && !webRTCReady) {
            webRTCManager.initialize()
            webRTCReady = true
        }
    }

    // ── Room Ended countdown ───────────────────────────────────────────────────
    LaunchedEffect(roomEnded) {
        if (roomEnded) {
            while (redirectCountdown > 0) {
                delay(1000)
                redirectCountdown--
            }
            navController.navigate("watch_party") {
                popUpTo("party_room/$roomId") { inclusive = true }
            }
        }
    }

    // ── Firestore room listener ────────────────────────────────────────────────
    LaunchedEffect(roomId) {
        firestore.collection("watch_parties").document(roomId)
            .addSnapshotListener { document, _ ->
                if (document != null && document.exists()) {
                    matchName = document.getString("matchName") ?: ""
                    matchInfo = document.getString("matchInfo") ?: ""
                    videoUrl = document.getString("videoUrl") ?: ""
                    hostId = document.getString("hostId") ?: ""

                    val participantsMap = document.get("participants") as? Map<*, *>
                    val newParticipants = participantsMap?.keys?.map { it.toString() } ?: emptyList()

                    // Detect newly joined peers and connect WebRTC
                    if (webRTCReady && webRTCJoined) {
                        val joined = newParticipants.filter { it !in prevParticipants && it != userId }
                        joined.forEach { newPeerId ->
                            webRTCManager.onNewPeerJoined(newPeerId)
                        }
                        val left = prevParticipants.filter { it !in newParticipants }
                        left.forEach { leftPeerId ->
                            webRTCManager.removePeer(leftPeerId)
                        }
                    }

                    prevParticipants = participants
                    participants = newParticipants

                    val isLive = document.getBoolean("isLive") ?: true
                    if (!isLive && userId != hostId) roomEnded = true

                } else {
                    if (userId != hostId) roomEnded = true
                    else {
                        navController.navigate("watch_party") {
                            popUpTo("party_room/$roomId") { inclusive = true }
                        }
                    }
                }
            }
    }

    // ── Join WebRTC room once ready and participants loaded ────────────────────
    LaunchedEffect(webRTCReady, participants) {
        if (webRTCReady && !webRTCJoined && participants.isNotEmpty()) {
            val existingPeers = participants.filter { it != userId }
            webRTCManager.joinRoom(roomId, userId, existingPeers)
            webRTCJoined = true
        }
    }

    // ── Fetch usernames ────────────────────────────────────────────────────────
    LaunchedEffect(participants) {
        participants.forEach { uid ->
            if (!userNames.containsKey(uid)) {
                firestore.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        val name = doc.getString("fullName")
                            ?: doc.getString("username")
                            ?: "User"
                        userNames = userNames + (uid to name)
                    }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { webRTCManager.release() }
    }

    // ── Room Ended Screen ──────────────────────────────────────────────────────
    if (roomEnded) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.VideocamOff, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(40.dp))
                }
                Text("Party Ended", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("The host has ended this watch party.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center)
                Text("Redirecting in $redirectCountdown seconds...",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        navController.navigate("watch_party") {
                            popUpTo("party_room/$roomId") { inclusive = true }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Back to Watch Party", fontWeight = FontWeight.Bold) }
            }
        }
        return
    }

    // ── Main Room UI ───────────────────────────────────────────────────────────
    val isHost = userId == hostId

    Box(
        modifier = Modifier.fillMaxSize()
            .onGloballyPositioned { coords ->
                screenWidth = coords.size.width.toFloat()
                screenHeight = coords.size.height.toFloat()
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(matchName, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold)
                        Text(
                            "🔴 Watch Party  •  ${participants.size} watching",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Text("Room: $roomId",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }

            // ✅ Synced video player — host controls, viewers follow
            if (videoUrl.isNotEmpty()) {
                SyncedVideoPlayer(
                    videoUrl = videoUrl,
                    roomId = roomId,
                    isHost = isHost,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
        }

        // ── Remote participant video tiles ─────────────────────────────────────
        val otherParticipants = participants.filter { it != userId }
        if (otherParticipants.isNotEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                otherParticipants.forEach { uid ->
                    val remoteTrack = remoteVideoTracks[uid]
                    RemoteParticipantTile(
                        name = userNames[uid] ?: "...",
                        isHost = uid == hostId,
                        videoTrack = remoteTrack,
                        eglBase = webRTCManager.eglBase
                    )
                }
            }
        }

        // ── My floating draggable camera bubble ───────────────────────────────
        if (permissionsGranted) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .size(bubbleSize)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidth - bubblePx)
                            offsetY = (offsetY + dragAmount.y).coerceIn(0f, screenHeight - bubblePx)
                        }
                    }
            ) {
                if (isCameraOn && webRTCReady) {
                    CameraPreview(webRTCManager = webRTCManager, modifier = Modifier.fillMaxSize())
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.VideocamOff, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                    }
                }

                val myName = userNames[userId] ?: "You"
                Text(
                    text = if (isHost) "👑 $myName (You)" else "$myName (You)",
                    color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Color(0x99000000)).padding(2.dp)
                )

                Row(modifier = Modifier.align(Alignment.TopCenter).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SmallControlButton(
                        icon = if (isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
                        isActive = isMicOn,
                        onClick = { isMicOn = !isMicOn; webRTCManager.setMicEnabled(isMicOn) }
                    )
                    SmallControlButton(
                        icon = if (isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        isActive = isCameraOn,
                        onClick = { isCameraOn = !isCameraOn; webRTCManager.setCameraEnabled(isCameraOn) }
                    )
                }
            }
        }

        // ── Leave / End Party ─────────────────────────────────────────────────
        Button(
            onClick = {
                if (isHost) {
                    firestore.collection("watch_parties").document(roomId)
                        .update("isLive", false)
                        .addOnSuccessListener {
                            firestore.collection("watch_parties").document(roomId).delete()
                        }
                } else {
                    firestore.collection("watch_parties").document(roomId)
                        .update("participants.$userId", com.google.firebase.firestore.FieldValue.delete())
                }
                webRTCManager.release()
                navController.navigate("watch_party") {
                    popUpTo("party_room/$roomId") { inclusive = true }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).height(44.dp)
        ) {
            Text(
                text = if (isHost) "End Party" else "Leave Party",
                color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Remote participant tile with real video track ─────────────────────────────
@Composable
fun RemoteParticipantTile(
    name: String,
    isHost: Boolean,
    videoTrack: VideoTrack?,
    eglBase: org.webrtc.EglBase
) {
    Box(
        modifier = Modifier
            .size(width = 90.dp, height = 70.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (videoTrack != null) {
            // Render remote video track
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    org.webrtc.SurfaceViewRenderer(ctx).apply {
                        init(eglBase.eglBaseContext, null)
                        setMirror(false)
                        setEnableHardwareScaler(true)
                        videoTrack.addSink(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // No video — show avatar
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
            }
        }

        Text(
            text = if (isHost) "👑 $name" else name,
            color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color(0x99000000)).padding(2.dp)
        )
    }
}

@Composable
fun SmallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp).clip(CircleShape)
            .background(if (isActive) Color(0x99000000) else MaterialTheme.colorScheme.error)
    ) {
        Icon(imageVector = icon, contentDescription = null,
            tint = Color.White, modifier = Modifier.size(14.dp))
    }
}