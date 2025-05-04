package com.example.maite.model

/**
 * 회원가입 API 응답 데이터 모델
 */
data class SignupResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: SignupResult
)

/**
 * 회원가입 결과 데이터 모델
 */
data class SignupResult(
    val userId: Int,
    val email: String,
    val name: String,
    val registeredAt: String,
    val message: String,
    val registered: Boolean
)