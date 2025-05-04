package com.example.maite.model

/**
 * 로그인 API 요청 데이터 모델
 */
data class LoginRequest(
    val email: String,
    val password: String
)