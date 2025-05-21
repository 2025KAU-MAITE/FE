package com.example.maite.model

/**
 * 소셜 로그인 회원가입 요청 데이터 모델
 */
data class SocialSignupRequest(
    val email: String,
    val name: String,
    val provider: String, // 제공자(GOOGLE)
    val phonenumber: String, // API 요구사항에 따라 phonenumber로 명명
    val address: String,
    val idToken: String? = null // idToken을 요청 바디에 포함할 수 있도록 추가
)

/**
 * 소셜 로그인 회원가입 응답 데이터 모델
 */
data class SocialSignupResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: SocialSignupResult
)

/**
 * 소셜 로그인 회원가입 결과 데이터 모델
 */
data class SocialSignupResult(
    val userId: Long,
    val email: String,
    val name: String,
    val registeredAt: String,
    val message: String,
    val registered: Boolean,
    val accessToken: String? = null,
    val idToken: String? = null
)
