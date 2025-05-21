package com.example.maite.model

import com.google.gson.annotations.SerializedName

/**
 * Google 로그인 API 요청 데이터 모델
 * 서버 API와 정확히 일치하는 필드명 사용 필수
 */
data class GoogleLoginRequest(
    @SerializedName("idToken") val idToken: String,
    @SerializedName("accessToken") val accessToken: String
)

/**
 * Google 로그인 API 응답 데이터 모델
 */
data class GoogleLoginResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: GoogleLoginResult
)

/**
 * Google 로그인 결과 데이터 모델
 */
data class GoogleLoginResult(
    val accessToken: String?,
    val message: String
)
