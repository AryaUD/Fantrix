package com.example.fantrix.Viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fantrix.data.FeedPost
import com.example.fantrix.data.FeedRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    private val repository = FeedRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _feedState = MutableStateFlow<FeedState>(FeedState.Loading)
    val feedState: StateFlow<FeedState> = _feedState

    init {
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            repository.getFeedPosts()
                .collect { posts ->
                    _feedState.value = FeedState.Success(posts)
                }
        }
    }

    fun createPost(content: String) {
        viewModelScope.launch {
            try {
                val user = auth.currentUser ?: return@launch
                val (username, handle, profileImage) = repository.getUserData(user.uid)

                val success = repository.createPost(
                    userId = user.uid,
                    username = username,
                    userHandle = handle,
                    userImageUrl = profileImage,
                    content = content
                )

                if (!success) {
                    _feedState.value = FeedState.Error("Failed to create post")
                }
            } catch (e: Exception) {
                _feedState.value = FeedState.Error(e.message ?: "Error")
            }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            repository.toggleLike(postId, userId)
        }
    }

    fun toggleRetweet(postId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            repository.toggleRetweet(postId, userId)
        }
    }
}

sealed class FeedState {
    object Loading : FeedState()
    data class Success(val posts: List<FeedPost>) : FeedState()
    data class Error(val message: String) : FeedState()
}