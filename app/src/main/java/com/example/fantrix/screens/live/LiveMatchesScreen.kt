package com.example.fantrix.screens.live

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

data class LiveMatch(
    val id: String,
    val title: String,
    val imageUrl: String
)

@Composable
fun LiveMatchesScreen(navController: NavController) {

    val liveMatches = listOf(
        LiveMatch(
            "1",
            "India vs Australia",
            "https://images.unsplash.com/photo-1509027572446-af8401acfdc3"
        ),
        LiveMatch(
            "2",
            "Real Madrid vs Barcelona",
            "https://images.unsplash.com/photo-1521412644187-c49fa049e84d"
        ),
        LiveMatch(
            "3",
            "Man United vs Arsenal",
            "https://images.unsplash.com/photo-1505842465776-3d90f616310d"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Watch Live",
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(liveMatches) { match ->
                LiveMatchCard(match) {
                    navController.navigate("live_player")
                }
            }
        }
    }
}

@Composable
fun LiveMatchCard(
    match: LiveMatch,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column {

            Box {
                Image(
                    painter = rememberAsyncImagePainter(match.imageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Crop
                )

                Text(
                    text = "WATCH LIVE",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = match.title,
                    fontSize = 14.sp
                )
            }
        }
    }
}
