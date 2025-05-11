package com.example.maite.model

data class GoogleLoginRequest(
    val idToken: String
)

data class GoogleLoginResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: GoogleLoginResult
)

data class GoogleLoginResult(
    val accessToken: String,
    val idToken: String,
    val message: String,
    val isRegistered: Boolean, // 이미 회원가입이 되어있는지 여부
    val email: String? = null,  // 이메일
    val name: String? = null    // 사용자 이름
)
