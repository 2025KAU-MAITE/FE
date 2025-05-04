package com.example.maite.model

/**
 * 로그인 API 응답 데이터 모델
 */
data class LoginResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: LoginResult
)

/**
 * 로그인 결과 데이터 모델
 */
data class LoginResult(
    val accessToken: String,
    val message: String
)