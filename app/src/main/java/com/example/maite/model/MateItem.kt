package com.example.maite.model

data class MateItem(
    val id: Long,
    val userId: Long,
    val name: String,
    val email: String?,
    val profileImageUrl: String?
)