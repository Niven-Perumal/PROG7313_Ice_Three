package com.niven.socialmediaapp

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Post(
    val userId: String = "",
    val userName: String = "",
    val imageBase64: String = "",
    val caption: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)