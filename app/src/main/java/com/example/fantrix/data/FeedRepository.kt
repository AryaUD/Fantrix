package com.example.fantrix.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FeedRepository {
    private val db = FirebaseFirestore.getInstance()
    private val postsCollection = db.collection("posts")
    private val usersCollection = db.collection("users")

    fun getFeedPosts(): Flow<List<FeedPost>> = callbackFlow {
        val listener = postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                snapshot?.let {
                    val posts = it.documents.map { doc ->
                        doc.toObject(FeedPost::class.java)?.copy(id = doc.id) ?: FeedPost()
                    }
                    trySend(posts)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun createPost(
        userId: String,
        username: String,
        userHandle: String,
        userImageUrl: String,
        content: String
    ): Boolean = try {
        val post = FeedPost(
            userId = userId,
            username = username,
            userHandle = userHandle,
            userImageUrl = userImageUrl,
            content = content
        )
        postsCollection.add(post).await()
        true
    } catch (e: Exception) {
        false
    }

    suspend fun toggleLike(postId: String, userId: String) {
        val postRef = postsCollection.document(postId)
        db.runTransaction { transaction ->
            val post = transaction.get(postRef).toObject(FeedPost::class.java)
            val currentLikes = post?.likes ?: emptyList()
            val updatedLikes = if (currentLikes.contains(userId)) {
                currentLikes - userId
            } else {
                currentLikes + userId
            }
            transaction.update(postRef, "likes", updatedLikes)
        }.await()
    }

    suspend fun toggleRetweet(postId: String, userId: String) {
        val postRef = postsCollection.document(postId)
        db.runTransaction { transaction ->
            val post = transaction.get(postRef).toObject(FeedPost::class.java)
            val currentRetweets = post?.retweets ?: emptyList()
            val updatedRetweets = if (currentRetweets.contains(userId)) {
                currentRetweets - userId
            } else {
                currentRetweets + userId
            }
            transaction.update(postRef, "retweets", updatedRetweets)
        }.await()
    }

    suspend fun getUserData(userId: String): Triple<String, String, String> {
        return try {
            val doc = usersCollection.document(userId).get().await()
            val username = doc.getString("fullName") ?: "User"
            val handle = doc.getString("email")?.split("@")?.first() ?: "user"
            val profileImage = doc.getString("profileImage") ?: ""
            Triple(username, "@$handle", profileImage)
        } catch (e: Exception) {
            Triple("User", "@user", "")
        }
    }
}