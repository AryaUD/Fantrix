package com.example.fantrix.screens.watchparty

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class LivePartyPreview(
    val roomId: String,
    val matchName: String,
    val matchInfo: String,
    val participantCount: Int,
    val isPublic: Boolean
)

@Composable
fun WatchPartyScreen(navController: NavController) {

    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var allLiveParties by remember { mutableStateOf<List<LivePartyPreview>>(emptyList()) }
    var liveCount by remember { mutableStateOf(0) }

    // ✅ Auto-delete stale rooms older than 3 hours on screen open
    LaunchedEffect(Unit) {
        firestore.collection("watch_parties")
            .get()
            .addOnSuccessListener { snapshot ->
                val cutoff = System.currentTimeMillis() - (3 * 60 * 60 * 1000)
                snapshot.documents.forEach { doc ->
                    val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0
                    if (createdAt < cutoff) {
                        doc.reference.delete()
                    }
                }
            }
    }

    // Fetch all live parties (real-time)
    LaunchedEffect(Unit) {
        firestore.collection("watch_parties")
            .whereEqualTo("isLive", true)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    liveCount = snapshot.size()
                    allLiveParties = snapshot.documents.mapNotNull { doc ->
                        val matchName = doc.getString("matchName") ?: return@mapNotNull null
                        val matchInfo = doc.getString("matchInfo") ?: ""
                        val participants = (doc.get("participants") as? Map<*, *>)?.size ?: 0
                        val isPublic = doc.getBoolean("isPublic") ?: false
                        LivePartyPreview(
                            roomId = doc.id,
                            matchName = matchName,
                            matchInfo = matchInfo,
                            participantCount = participants,
                            isPublic = isPublic
                        )
                    }
                }
            }
    }

    // Separate public and private parties
    val publicParties = allLiveParties.filter { it.isPublic }

    // Pulse animation for live dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {

        // ── Hero Section ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0D1B2A),
                                Color(0xFF1B2A4A),
                                Color(0xFF0D2137)
                            )
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x331A6BFF), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.1f),
                            radius = size.width * 0.7f
                        )
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x990D1B2A)),
                            startY = size.height * 0.5f,
                            endY = size.height
                        )
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val lineColor = Color(0x0FFFFFFF)
                for (i in 0..8) {
                    val x = size.width * i / 8f
                    drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), 1f)
                }
                for (i in 0..5) {
                    val y = size.height * i / 5f
                    drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 1f)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 48.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B3B).copy(alpha = pulseAlpha))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (publicParties.isNotEmpty()) "${publicParties.size} PUBLIC PARTIES LIVE" else "WATCH TOGETHER",
                        color = Color(0xFFFF3B3B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                Column {
                    Text(
                        text = "Watch",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Light,
                        lineHeight = 42.sp
                    )
                    Text(
                        text = "Party",
                        color = Color.White,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 52.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Watch live matches with friends,\nanywhere in the world.",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // ── Live Now (horizontal scroll — public rooms only) ─────────────────
        if (publicParties.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 24.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Now",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFF3B3B)
                    ) {
                        Text(
                            text = "${publicParties.size}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ✅ Only show public parties here
                    items(publicParties) { party ->
                        LivePartyCard(
                            party = party,
                            onClick = {
                                val uid = currentUserId ?: return@LivePartyCard
                                FirebaseFirestore.getInstance()
                                    .collection("watch_parties")
                                    .document(party.roomId)
                                    .update("participants.$uid", true)
                                    .addOnSuccessListener {
                                        navController.navigate("party_room/${party.roomId}")
                                    }
                            }
                        )
                    }
                }
            }
        }

        // ── Public Rooms Section ──────────────────────────────────────────────
        if (publicParties.isEmpty() && liveCount > 0) {
            // There are rooms but all are private
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Open to Everyone",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "No public rooms available",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "All active rooms are private right now",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        if (publicParties.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Open to Everyone",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Jump into a public room — no password needed",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(Modifier.height(12.dp))

                publicParties.forEach { party ->
                    PublicRoomRow(
                        party = party,
                        onClick = {
                            val uid = currentUserId ?: return@PublicRoomRow
                            FirebaseFirestore.getInstance()
                                .collection("watch_parties")
                                .document(party.roomId)
                                .update("participants.$uid", true)
                                .addOnSuccessListener {
                                    navController.navigate("party_room/${party.roomId}")
                                }
                        }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        // ── Get Started Cards ─────────────────────────────────────────────────
        // ── No public rooms at all ────────────────────────────────────────────
        if (liveCount == 0) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Open to Everyone",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "No public rooms available",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Be the first to host a public watch party!",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Host Party
            Card(
                onClick = { navController.navigate("live_matches_host") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF1A3A6B), Color(0xFF0F2447)),
                                    start = Offset.Zero,
                                    end = Offset(size.width, size.height)
                                )
                            )
                        }
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x331A6BFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color(0xFF5B9BFF),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Host a Party",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Pick a match, choose public or private,\nand invite your friends",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Join Private Party
            Card(
                onClick = { navController.navigate("join_party") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF1A3A2A), Color(0xFF0F2418)),
                                    start = Offset.Zero,
                                    end = Offset(size.width, size.height)
                                )
                            )
                        }
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x2200C85A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF00C85A),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Join Private Party",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Enter a room code and password\nto join a private room",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // ── How It Works ──────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 32.dp)
        ) {
            Text(
                text = "How it works",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HowItWorksStep(number = "1", title = "Host", subtitle = "Pick a match\n& set type")
                StepConnector()
                HowItWorksStep(number = "2", title = "Share", subtitle = "Invite friends\nor go public")
                StepConnector()
                HowItWorksStep(number = "3", title = "Watch", subtitle = "Watch together\nwith video chat")
            }
        }
    }
}

// ── Live Party Card (horizontal scroll) ──────────────────────────────────────
@Composable
fun LivePartyCard(party: LivePartyPreview, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B3B))
                    )
                    Text(
                        text = "LIVE",
                        color = Color(0xFFFF3B3B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                // Public/Private badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (party.isPublic)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (party.isPublic) "Public" else "Private",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (party.isPublic)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = party.matchName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = party.matchInfo,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${party.participantCount} watching",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                if (party.isPublic) {
                    Text(
                        text = "Tap to join",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Public Room Row (full width list) ────────────────────────────────────────
@Composable
fun PublicRoomRow(party: LivePartyPreview, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LiveTv,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = party.matchName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${party.participantCount} watching • Public",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Join", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// ── How It Works Step ─────────────────────────────────────────────────────────
@Composable
fun HowItWorksStep(number: String, title: String, subtitle: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(3.dp))
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            lineHeight = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ── Step Connector ────────────────────────────────────────────────────────────
@Composable
fun StepConnector() {
    Box(
        modifier = Modifier
            .padding(top = 22.dp)
            .width(24.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    )
}