package com.example.maite.model

data class GoogleLoginRequest(
    val idToken: String
)

data class GoogleLoginResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: LoginResult
)
