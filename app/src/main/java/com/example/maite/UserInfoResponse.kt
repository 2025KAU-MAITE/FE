package com.example.maite

data class UserInfoResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: UserInfoResult
)

data class UserInfoResult(
    val userId: Long,
    val email: String,
    val name: String,
    val profileImageUrl: String? = null,
    val subscribed: Boolean = false  // 요금제 상태: false = 베이직, true = 프리미엄
)