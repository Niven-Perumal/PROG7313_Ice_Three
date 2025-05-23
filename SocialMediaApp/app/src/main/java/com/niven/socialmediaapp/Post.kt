package com.niven.socialmediaapp

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Post(
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val imageBase64: String = "",
    val caption: String = "",
    val likes: Int = 0,
    val likedBy: List<String> = emptyList(),
    @ServerTimestamp val timestamp: Timestamp? = null
)