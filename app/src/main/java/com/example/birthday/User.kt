package com.example.birthday

// User data class for Firebase
data class User(
    val name: String = "",         // Default value for Firebase deserialization
    val email: String = "",        // Default value for Firebase deserialization
    val profilePhoto: String = ""  // Default value for Firebase deserialization
)
