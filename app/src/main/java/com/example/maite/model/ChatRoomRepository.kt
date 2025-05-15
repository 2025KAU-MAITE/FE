package com.example.maite.model

import java.util.UUID

class ChatRoomRepository {

    // 채팅방 정보 가져오기
    fun getChatRoomInfo(chatId: String): ChatRoom {
        // 실제로는 서버나 DB에서 정보를 가져옴
        return ChatRoom(
            id = chatId,
            name = if (chatId.startsWith("g")) "프로젝트 회의" else "김정훈",
            profileImageUrl = null,
            isGroup = chatId.startsWith("g"),
            participants = listOf("user1", "user2", "user3")
        )
    }

    // 메시지 목록 가져오기
    fun getMessages(chatId: String): List<Message> {
        val currentTime = System.currentTimeMillis()
        // 1분 간격으로 메시지 생성 (데모용)
        val minuteMillis = 60 * 1000L

        // 실제로는 서버나 DB에서 정보를 가져옴
        return if (chatId.startsWith("g")) {
            // 단체 채팅 메시지
            listOf(
                Message(
                    id = UUID.randomUUID().toString(),
                    senderId = "user1",
                    senderName = "사람 1",
                    content = "채팅 내용 1",
                    timestamp = currentTime - 5 * minuteMillis
                ),
                Message(
                    id = UUID.randomUUID().toString(),
                    senderId = "user1",
                    senderName = "사람 1",
                    content = "채팅 내용 2",
                    timestamp = currentTime - 4 * minuteMillis
                ),
                Message(
                    id = UUID.randomUUID().toString(),
                    senderId = "user2",
                    senderName = "사람 2",
                    content = "채팅 내용 1",
                    timestamp = currentTime - 3 * minuteMillis
                ),
                Message(
                    id = UUID.randomUUID().toString(),
                    senderId = "user2",
                    senderName = "사람 2",
                    content = "채팅 내용 2",
                    timestamp = currentTime - 2 * minuteMillis
                ),
                Message(
                    id = UUID.randomUUID().toString(),
                    senderId = "current_user_id",
                    senderName = "나",
                    content = "채팅 내용 2",
                    timestamp = currentTime - minuteMillis
                ),
                Message(
                    id = UUID.randomUUID().toString(),
                    senderId = "current_user_id",
                    senderName = "나",
                    content = "채팅 내용 2",
                    timestamp = currentTime
                )
            )
        } else {
            // 개인 채팅 메시지
            listOf(
                Message(
                    id = UUID.randomUUID().toString(),
                    senderId = chatId,
                    senderName = "김정훈",
                    content = "안녕하세요!",
                    timestamp = currentTime - 3 * minuteMillis
                ),
                Message(
                    id = UUID.randomUUID().toString(),
                    senderId = "current_user_id",
                    senderName = "나",
                    content = "네, 안녕하세요!",
                    timestamp = currentTime - 2 * minuteMillis
                ),
                Message(
                    id = UUID.randomUUID().toString(),
                    senderId = chatId,
                    senderName = "김정훈",
                    content = "프로젝트는 잘 진행되고 있나요?",
                    timestamp = currentTime - minuteMillis
                ),
                Message(
                    id = UUID.randomUUID().toString(),
                    senderId = "current_user_id",
                    senderName = "나",
                    content = "네, 잘 진행되고 있습니다!",
                    timestamp = currentTime
                )
            )
        }
    }

    // 메시지 전송
    fun sendMessage(chatId: String, content: String): Message {
        // 실제로는 서버에 메시지 전송 후 응답 받음
        return Message(
            id = UUID.randomUUID().toString(),
            senderId = "current_user_id",
            senderName = "나",
            content = content,
            timestamp = System.currentTimeMillis()
        )
    }
}

data class ChatRoom(
    val id: String,
    val name: String,
    val profileImageUrl: String? = null,
    val isGroup: Boolean,
    val participants: List<String>
)