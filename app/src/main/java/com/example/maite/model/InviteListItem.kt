package com.example.maite.model

data class InviteListItem(
    val id: Long,
    val name: String,
    val email: String? = null,
    val profileImageUrl: String? = null
)