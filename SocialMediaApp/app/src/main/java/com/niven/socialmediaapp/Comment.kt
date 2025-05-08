package com.niven.socialmediaapp

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Comment(
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val userAvatar: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)