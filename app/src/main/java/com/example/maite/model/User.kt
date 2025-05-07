package com.example.maite.model

data class User(
    val id: String,
    val name: String,
    val email: String, // Added email field
    val profileImageUrl: String,
    var isSelected: Boolean = false
)