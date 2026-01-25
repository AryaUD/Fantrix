package com.example.fantrix.data

import com.google.firebase.Timestamp

data class FeedPost(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userHandle: String = "",
    val userImageUrl: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val likes: List<String> = emptyList(),
    val retweets: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now()
)