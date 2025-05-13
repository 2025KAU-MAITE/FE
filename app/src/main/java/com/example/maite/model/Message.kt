package com.example.maite.model

data class Message(
    val id: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val type: MessageType = MessageType.TEXT
)

enum class MessageType {
    TEXT, IMAGE, FILE
}