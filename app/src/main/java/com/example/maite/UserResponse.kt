package com.example.maite

data class UserResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: List<UserResult> // UserInfoResult -> UserResult
)

// 클래스 이름을 UserResult로 변경
data class UserResult(
    val id: Long, // API 응답 필드명 'id'에 맞춤 (기존 userId에서 변경)
    val email: String,
    val name: String,
    val profileImageUrl: String? = null,
    val subscribed: Boolean = false,
    val pendingSent: Boolean? = null,
    val pendingReceived: Boolean? = null,
    val mate: Boolean? = null
)