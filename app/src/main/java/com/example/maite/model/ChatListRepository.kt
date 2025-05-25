package com.example.maite.model

import android.content.Context
import com.example.maite.MaiteRetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ChatListRepository(private val context: Context) {

    private val apiService = MaiteRetrofitClient.getInstance(context)

    // 채팅방 목록을 API에서 가져오기
    suspend fun getChatRooms(): List<ChatListItem> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getChatRooms()
            if (response.isSuccessful && response.body()?.isSuccess == true) {
                val chatRooms = response.body()?.result ?: emptyList()
                return@withContext chatRooms.map { dto -> dto.toChatListItem() }
            } else {
                throw Exception("채팅방 목록을 불러오는데 실패했습니다: ${response.message()}")
            }
        } catch (e: Exception) {
            throw Exception("네트워크 오류: ${e.message}")
        }
    }

    // 개인 채팅 목록 가져오기
    suspend fun getPersonalChats(): List<ChatListItem> {
        val allChats = getChatRooms()
        return allChats.filter { !it.isGroup }
    }

    // 단체 채팅 목록 가져오기
    suspend fun getGroupChats(): List<ChatListItem> {
        val allChats = getChatRooms()
        return allChats.filter { it.isGroup }
    }

    // 검색어로 채팅방 찾기
    suspend fun searchChats(query: String, isPersonal: Boolean): List<ChatListItem> {
        val chatList = if (isPersonal) getPersonalChats() else getGroupChats()
        return chatList.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.lastMessage?.contains(query, ignoreCase = true) == true
        }
    }
}

// DTO를 ChatListItem으로 변환하는 확장 함수
private fun ChatRoomDto.toChatListItem(): ChatListItem {
    return ChatListItem(
        id = this.id.toString(),
        name = this.roomName,
        profileImageUrl = this.profileImageUrl,
        lastMessage = this.lastMessageContent,
        intro = if (this.isGroupChat) "참여자 ${this.participantCount}명" else null,
        timestamp = parseTimestamp(this.lastMessageTime),
        isGroup = this.isGroupChat
    )
}

// ISO 8601 형식의 시간을 timestamp로 변환
private fun parseTimestamp(timeString: String?): Long? {
    return try {
        timeString?.let {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(it)?.time
        }
    } catch (e: Exception) {
        null
    }
}