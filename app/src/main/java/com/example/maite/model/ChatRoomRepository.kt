package com.example.maite.model

import android.content.Context
import android.util.Log
import com.example.maite.MaiteRetrofitClient
import com.example.maite.PreferencesUtil
import com.example.maite.network.WebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ChatRoomRepository(private val context: Context) {

    private val TAG = "ChatRoomRepository"
    private val apiService = MaiteRetrofitClient.getInstance(context)
    private val preferencesUtil = PreferencesUtil(context)
    private val webSocketManager = WebSocketManager.getInstance()

    // 메시지 목록을 API에서 가져오기
    suspend fun getMessages(chatId: String, lastMessageId: Long? = null): List<Message> = withContext(Dispatchers.IO) {
        try {
            val roomId = chatId.toLongOrNull() ?: throw Exception("잘못된 채팅방 ID입니다")
            Log.d(TAG, "메시지 가져오기 요청: roomId=$roomId, lastMessageId=$lastMessageId")

            val response = apiService.getChatMessages(roomId, lastMessageId)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val messages = response.body()?.result ?: emptyList()
                Log.d(TAG, "메시지 가져오기 성공: ${messages.size}개")
                return@withContext messages.map { dto -> dto.toMessage() }
            } else {
                Log.e(TAG, "메시지 가져오기 실패: ${response.code()} - ${response.message()}")
                throw Exception("메시지를 불러오는데 실패했습니다: ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "메시지 가져오기 중 오류: ${e.message}", e)
            throw Exception("네트워크 오류: ${e.message}")
        }
    }

    // 채팅방 구독 및 메시지 수신 리스너 설정
    fun subscribeToChatRoom(chatId: String, onMessageReceived: (Message) -> Unit) {
        Log.d(TAG, "채팅방 구독 설정: $chatId")
        // WebSocket 연결 확인 및 연결
        if (!webSocketManager.isConnected()) {
            Log.d(TAG, "WebSocket 연결 필요 - 연결 시도")
            webSocketManager.connect()
        }

        // 채팅방 구독
        webSocketManager.subscribeToChatRoom(chatId, onMessageReceived)
    }

    // 채팅방 구독 해제
    fun unsubscribeFromChatRoom(chatId: String, onMessageReceived: (Message) -> Unit) {
        Log.d(TAG, "채팅방 구독 해제: $chatId")
        webSocketManager.unsubscribeFromChatRoom(chatId, onMessageReceived)
    }

    // WebSocket을 통한 메시지 전송
    fun sendMessage(chatId: String, content: String) {
        if (content.isBlank()) return

        // 실제 WebSocket을 통한 메시지 전송
        webSocketManager.sendMessage(chatId, content)
    }

    // 메시지 읽음 처리
    fun markMessageAsRead(chatId: String, messageId: String) {
        webSocketManager.markMessageAsRead(chatId, messageId)
    }
}

// DTO를 Message로 변환하는 확장 함수
private fun MessageDto.toMessage(): Message {
    val timestamp = parseTimestamp(this.sendAt)

    // 로그 추가
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    sdf.timeZone = TimeZone.getDefault() // 로컬 시간대로 표시
    Log.d("ChatRepo", "메시지 ID: ${this.id}, 원본 시간: ${this.sendAt}, 변환 시간: ${sdf.format(Date(timestamp))}")

    return Message(
        id = this.id.toString(),
        roomId = this.roomId.toString(),
        senderId = this.senderId.toString(),
        senderName = this.senderName,
        senderProfileImageUrl = this.senderProfileImageUrl,
        content = this.content,
        imageUrl = this.imageUrl,
        timestamp = timestamp,
        readCount = this.readCount,
        totalMemberCount = this.totalMemberCount,
        isRead = this.isRead,
        type = if (this.imageUrl != null) MessageType.IMAGE else MessageType.TEXT
    )
}

private fun parseTimestamp(timeString: String): Long {
    if (timeString.isEmpty()) {
        return System.currentTimeMillis()
    }

    try {
        // 마이크로초 6자리 포맷 (Z 없음)
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")

        // 디버그 로그 추가
        Log.d("TimeDebug", "파싱 시도(6자리): $timeString")

        val parsedDate = format.parse(timeString)
        if (parsedDate != null) {
            val timestamp = parsedDate.time
            Log.d("TimeDebug", "파싱 성공(6자리): $timeString -> ${Date(timestamp)}")
            return timestamp
        }
    } catch (e: Exception) {
        Log.e("TimeDebug", "6자리 파싱 실패: ${e.message}")
    }

    try {
        // 밀리초 3자리 포맷 (Z 없음)
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")

        Log.d("TimeDebug", "파싱 시도(3자리): $timeString")

        val parsedDate = format.parse(timeString)
        if (parsedDate != null) {
            val timestamp = parsedDate.time
            Log.d("TimeDebug", "파싱 성공(3자리): $timeString -> ${Date(timestamp)}")
            return timestamp
        }
    } catch (e: Exception) {
        Log.e("TimeDebug", "3자리 파싱 실패: ${e.message}")
    }

    try {
        // 밀리초 없는 포맷
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")

        Log.d("TimeDebug", "파싱 시도(밀리초 없음): $timeString")

        val parsedDate = format.parse(timeString)
        if (parsedDate != null) {
            val timestamp = parsedDate.time
            Log.d("TimeDebug", "파싱 성공(밀리초 없음): $timeString -> ${Date(timestamp)}")
            return timestamp
        }
    } catch (e: Exception) {
        Log.e("TimeDebug", "밀리초 없는 파싱 실패: ${e.message}")
    }

    // 모든 파싱 시도 실패 시 현재 시간 사용
    Log.e("TimeDebug", "모든 시간 파싱 실패, 현재 시간 사용: $timeString")
    return System.currentTimeMillis()
}