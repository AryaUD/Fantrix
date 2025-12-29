package com.example.fantrix.screens.live

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlin.random.Random

/* ---------------- DATA MODELS ---------------- */

data class ChatMessage(
    val userName: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null
)

data class Reaction(
    val emoji: String = "",
    val timestamp: Timestamp? = null
)

/* ---------------- SCREEN ---------------- */

@Composable
fun LivePlayerScreen() {

    val context = LocalContext.current
    val activity = context as Activity
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var isFullscreen by remember { mutableStateOf(false) }

    val userName = remember {
        auth.currentUser?.displayName
            ?: auth.currentUser?.email
            ?: "Guest"
    }

    /* ---------------- VIDEO PLAYER ---------------- */

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    LaunchedEffect(Unit) {
        exoPlayer.setMediaItem(
            MediaItem.fromUri("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
        )
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(Unit) {
        onDispose {
            activity.requestedOrientation =
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            exoPlayer.release()
        }
    }

    /* ---------------- CHAT ---------------- */

    var chatInput by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }

    DisposableEffect(Unit) {
        val listener = firestore.collection("live_chats")
            .document("global")
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                messages.clear()
                snapshot.documents.forEach {
                    it.toObject(ChatMessage::class.java)?.let(messages::add)
                }
            }
        onDispose { listener.remove() }
    }

    /* ---------------- REACTIONS ---------------- */

    val reactions = remember { mutableStateListOf<Reaction>() }

    DisposableEffect(Unit) {
        val listener = firestore.collection("live_chats")
            .document("global")
            .collection("reactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                reactions.clear()
                snapshot.documents.forEach {
                    it.toObject(Reaction::class.java)?.let(reactions::add)
                }
            }
        onDispose { listener.remove() }
    }

    /* ---------------- UI ---------------- */

    Column(modifier = Modifier.fillMaxSize()) {

        // 🎥 VIDEO PLAYER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFullscreen) 400.dp else 250.dp)
        ) {

            AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        useController = true
                    }
                }
            )

            // ⛶ FULLSCREEN BUTTON (BOTTOM-RIGHT — YOUTUBE STYLE)
            IconButton(
                onClick = {
                    isFullscreen = !isFullscreen
                    activity.requestedOrientation =
                        if (isFullscreen)
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        else
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 48.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Text("⛶", color = Color.White, fontSize = 18.sp)
            }

            // FLOATING REACTIONS
            reactions.forEach {
                FloatingEmoji(it.emoji)
            }
        }

        if (!isFullscreen) {

            // ❤️🔥😂 REACTIONS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ReactionButton("❤️", firestore)
                ReactionButton("🔥", firestore)
                ReactionButton("😂", firestore)
            }

            // 💬 CHAT
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                items(messages) { msg ->
                    Text(
                        text = "${msg.userName}: ${msg.text}",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // ✍️ INPUT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                TextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add a comment…") }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (chatInput.isNotBlank()) {
                            firestore.collection("live_chats")
                                .document("global")
                                .collection("messages")
                                .add(
                                    ChatMessage(
                                        userName = userName,
                                        text = chatInput,
                                        timestamp = Timestamp.now()
                                    )
                                )
                            chatInput = ""
                        }
                    }
                ) {
                    Text("Send")
                }
            }
        }
    }
}

/* ---------------- FLOATING EMOJI ---------------- */

@Composable
fun FloatingEmoji(emoji: String) {
    val offsetY = remember { Animatable(200f) }
    val startX = remember { Random.nextInt(20, 300).toFloat() }

    LaunchedEffect(Unit) {
        offsetY.animateTo(-100f, tween(2000))
    }

    Text(
        text = emoji,
        fontSize = 32.sp,
        modifier = Modifier.offset(x = startX.dp, y = offsetY.value.dp)
    )
}

/* ---------------- REACTION BUTTON ---------------- */

@Composable
fun ReactionButton(
    emoji: String,
    firestore: FirebaseFirestore
) {
    Button(
        onClick = {
            firestore.collection("live_chats")
                .document("global")
                .collection("reactions")
                .add(
                    Reaction(
                        emoji = emoji,
                        timestamp = Timestamp.now()
                    )
                )
        }
    ) {
        Text(text = emoji, fontSize = 20.sp)
    }
}
