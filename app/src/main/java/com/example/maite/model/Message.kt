package com.example.maite.model

data class Message(
    val id: String,
    val roomId: String,
    val senderId: String,
    val senderName: String,
    val senderProfileImageUrl: String? = null,
    val content: String,
    val imageUrl: String? = null,
    val timestamp: Long,
    val readCount: Int = 0,
    val totalMemberCount: Int = 0,
    val isRead: Boolean = false,
    val type: MessageType = MessageType.TEXT
)

enum class MessageType {
    TEXT, IMAGE, FILE
}