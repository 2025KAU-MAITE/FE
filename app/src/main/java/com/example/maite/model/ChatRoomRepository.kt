package com.example.maite.model

import android.content.Context
import com.example.maite.MaiteRetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ChatRoomRepository(private val context: Context) {

    private val apiService = MaiteRetrofitClient.getInstance(context)

    // 메시지 목록을 API에서 가져오기
    suspend fun getMessages(chatId: String, lastMessageId: Long? = null): List<Message> = withContext(Dispatchers.IO) {
        try {
            val roomId = chatId.toLongOrNull() ?: throw Exception("잘못된 채팅방 ID입니다")
            val response = apiService.getChatMessages(roomId, lastMessageId)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val messages = response.body()?.result ?: emptyList()
                return@withContext messages.map { dto -> dto.toMessage() }
            } else {
                throw Exception("메시지를 불러오는데 실패했습니다: ${response.message()}")
            }
        } catch (e: Exception) {
            throw Exception("네트워크 오류: ${e.message}")
        }
    }

    // 메시지 전송 (API 구현 필요시 추가)
    suspend fun sendMessage(chatId: String, content: String): Message = withContext(Dispatchers.IO) {
        // 현재는 더미 데이터로 응답
        // 실제로는 메시지 전송 API 호출 후 응답 받기
        return@withContext Message(
            id = System.currentTimeMillis().toString(),
            roomId = chatId,
            senderId = "current_user_id", // 실제로는 현재 사용자 ID
            senderName = "나",
            content = content,
            timestamp = System.currentTimeMillis(),
            type = MessageType.TEXT
        )
    }
}

// DTO를 Message로 변환하는 확장 함수
private fun MessageDto.toMessage(): Message {
    return Message(
        id = this.id.toString(),
        roomId = this.roomId.toString(),
        senderId = this.senderId.toString(),
        senderName = this.senderName,
        senderProfileImageUrl = this.senderProfileImageUrl,
        content = this.content,
        imageUrl = this.imageUrl,
        timestamp = parseTimestamp(this.sendAt) ?: System.currentTimeMillis(),
        readCount = this.readCount,
        totalMemberCount = this.totalMemberCount,
        isRead = this.isRead,
        type = if (this.imageUrl != null) MessageType.IMAGE else MessageType.TEXT
    )
}

// ISO 8601 형식의 시간을 timestamp로 변환
private fun parseTimestamp(timeString: String): Long? {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.parse(timeString)?.time
    } catch (e: Exception) {
        try {
            // 밀리초 없는 형태도 시도
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(timeString)?.time
        } catch (e2: Exception) {
            null
        }
    }
}