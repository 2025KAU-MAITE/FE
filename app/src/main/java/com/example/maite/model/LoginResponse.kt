package com.example.maite.model

data class LoginResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: LoginResult
)

data class LoginResult(
    val accessToken: String,
    val message: String,
    val userId: Long = 0
)