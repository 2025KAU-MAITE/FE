package com.example.maite.model

data class ChatListItem(
    val id: String,
    val name: String,
    val profileImageUrl: String? = null,
    val lastMessage: String? = null,
    val intro: String? = null,
    val timestamp: Long? = null,
    val isGroup: Boolean = false
)