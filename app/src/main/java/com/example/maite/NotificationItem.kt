package com.example.maite.model

data class NotificationItem(
    val id: Int,
    val type: NotificationType,
    val senderName: String,
    val message: String,
    val profileImageRes: Int
)

enum class NotificationType {
    FRIEND_REQUEST, MEETING_INVITE, CHAT
}
