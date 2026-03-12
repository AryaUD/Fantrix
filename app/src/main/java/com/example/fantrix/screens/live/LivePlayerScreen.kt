package com.example.fantrix.screens.live

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

data class ChatMessage(val user: String, val text: String)

@Composable
fun LivePlayerScreen(
    videoUrl: String,
    matchName: String,
    matchInfo: String,
    matchId: String
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isFullscreen by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableStateOf("Auto") }
    val qualityOptions = listOf("Auto", "1080p", "720p", "480p", "360p")

    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            // Restore portrait on leave
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    // Handle fullscreen orientation
    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    val messages = remember(matchId) {
        mutableStateListOf(ChatMessage("System", "Welcome to $matchName chat 👋"))
    }
    var chatInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Video Player ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isFullscreen) Modifier.fillMaxSize()
                    else Modifier.aspectRatio(16f / 9f)
                )
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // ── Fullscreen button ─────────────────────────────────────────
            IconButton(
                onClick = { isFullscreen = !isFullscreen },
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
            ) {
                Icon(
                    imageVector = if (isFullscreen)
                        Icons.Default.FullscreenExit
                    else
                        Icons.Default.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }

            // ── Quality button ────────────────────────────────────────────
            Box(modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)) {
                IconButton(onClick = { showQualityMenu = true }) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Quality",
                        tint = androidx.compose.ui.graphics.Color.White
                    )
                }
                DropdownMenu(
                    expanded = showQualityMenu,
                    onDismissRequest = { showQualityMenu = false }
                ) {
                    qualityOptions.forEach { quality ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(quality)
                                    if (quality == selectedQuality) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "✓",
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            onClick = {
                                selectedQuality = quality
                                showQualityMenu = false
                                // Apply quality track selection
                                val trackGroups = exoPlayer.currentTracks.groups
                                if (quality == "Auto") {
                                    exoPlayer.trackSelectionParameters =
                                        exoPlayer.trackSelectionParameters
                                            .buildUpon()
                                            .clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_VIDEO)
                                            .build()
                                } else {
                                    val targetHeight = quality.replace("p", "").toIntOrNull() ?: 720
                                    for (group in trackGroups) {
                                        if (group.type == androidx.media3.common.C.TRACK_TYPE_VIDEO) {
                                            for (i in 0 until group.length) {
                                                val format = group.getTrackFormat(i)
                                                if (format.height == targetHeight) {
                                                    exoPlayer.trackSelectionParameters =
                                                        exoPlayer.trackSelectionParameters
                                                            .buildUpon()
                                                            .setOverrideForType(
                                                                TrackSelectionOverride(group.mediaTrackGroup, i)
                                                            )
                                                            .build()
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Hide everything else in fullscreen
        if (!isFullscreen) {

            // ── Match Info ────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = matchName, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = matchInfo, style = MaterialTheme.typography.bodyMedium)
            }

            Divider()

            // ── Live Chat ─────────────────────────────────────────────────────
            Text(
                text = "Live Chat",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(username = msg.user, message = msg.text)
                }
            }

            // ── Chat Input ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = chatInput,
                    onValueChange = { chatInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Say something…") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (chatInput.isNotBlank()) {
                            messages.add(ChatMessage("You", chatInput))
                            chatInput = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
fun ChatBubble(username: String, message: String) {
    Column {
        Text(text = username, style = MaterialTheme.typography.labelMedium)
        Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
            Text(
                text = message,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}