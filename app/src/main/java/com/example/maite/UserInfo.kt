package com.example.maite.model

data class UserInfo(
    val name: String,
    val mateCount: Int,
    val profileImageUrl: String? = null // TODO: 실제 이미지 URL 받아오도록 수정
)
