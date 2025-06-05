package com.example.maite.model

data class CreateGroupChatRequest(
    val roomName: String,
    val memberIds: List<Long>,
    val profileImageUrl: String? = null,
    val maxMembers: Int = 100  // 기본값 설정
)

data class ChatRoomCreateResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: ChatRoomDto
)