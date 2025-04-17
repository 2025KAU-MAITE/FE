package com.example.maite.model

data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: User?
)