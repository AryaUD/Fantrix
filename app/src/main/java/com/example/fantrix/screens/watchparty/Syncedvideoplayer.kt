package com.example.fantrix.screens.watchparty

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * SyncedVideoPlayer
 *
 * HOST:   Controls playback normally. Every 3 seconds pushes
 *         { position, isPlaying, updatedAt } to Firestore.
 *
 * VIEWER: Reads playback state from Firestore in real-time.
 *         If their position drifts >3s from host, seeks to host position.
 *         Mirrors host play/pause state.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun SyncedVideoPlayer(
    videoUrl: String,
    roomId: String,
    isHost: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    // ── HOST: push playback state to Firestore every 3 seconds ───────────────
    if (isHost) {
        LaunchedEffect(player) {
            while (isActive) {
                delay(3000)
                val state = mapOf(
                    "position" to player.currentPosition,
                    "isPlaying" to player.isPlaying,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection("watch_parties")
                    .document(roomId)
                    .update("playbackState", state)
            }
        }
    }

    // ── VIEWER: listen to Firestore and sync ──────────────────────────────────
    if (!isHost) {
        LaunchedEffect(Unit) {
            firestore.collection("watch_parties")
                .document(roomId)
                .addSnapshotListener { doc, _ ->
                    val state = doc?.get("playbackState") as? Map<*, *> ?: return@addSnapshotListener
                    val hostPosition = (state["position"] as? Long) ?: return@addSnapshotListener
                    val hostIsPlaying = (state["isPlaying"] as? Boolean) ?: true
                    val updatedAt = (state["updatedAt"] as? Long) ?: return@addSnapshotListener

                    // Compensate for network delay
                    val networkDelay = System.currentTimeMillis() - updatedAt
                    val adjustedPosition = hostPosition + networkDelay

                    // Sync play/pause
                    if (hostIsPlaying && !player.isPlaying) player.play()
                    if (!hostIsPlaying && player.isPlaying) player.pause()

                    // Seek if drift > 3 seconds
                    val drift = Math.abs(player.currentPosition - adjustedPosition)
                    if (drift > 3000) {
                        player.seekTo(adjustedPosition)
                    }
                }
        }
    }

    // ── Player UI ─────────────────────────────────────────────────────────────
    Box(modifier = modifier) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    this.player = player
                    useController = isHost // only host has controls
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isHost) {
            // Viewer label
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Synced with host",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}