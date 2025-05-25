package com.example.maite.model

data class UserInfo(
    val name: String,
    val mateCount: Int,
    val profileImageUrl: String?,
    val subscribed: Boolean = false  // 요금제 상태: false = 베이직, true = 프리미엄
)