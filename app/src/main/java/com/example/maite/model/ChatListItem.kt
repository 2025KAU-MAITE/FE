package com.example.maite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatListItem(
    val id: String,
    val name: String,
    val profileImageUrl: String? = null,
    val lastMessage: String? = null,
    val intro: String? = null,
    val timestamp: Long? = null,
    val isGroup: Boolean = false,
    val isUser: Boolean = false
) : Parcelable