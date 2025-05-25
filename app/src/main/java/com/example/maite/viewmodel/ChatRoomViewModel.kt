package com.example.maite.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.maite.PreferencesUtil
import com.example.maite.model.ChatRoomRepository
import com.example.maite.model.Message
import com.example.maite.model.MessageType
import kotlinx.coroutines.launch

class ChatRoomViewModel(
    private val chatId: String,
    private val repository: ChatRoomRepository,
    private val preferencesUtil: PreferencesUtil
) : ViewModel() {

    private val TAG = "ChatRoomViewModel"

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        Log.d(TAG, "ChatRoomViewModel 초기화 - 채팅방: $chatId")

        // 초기 메시지 로드
        loadMessages()

        // 채팅방 구독 설정 - 람다식 대신 함수 참조 사용
        repository.subscribeToChatRoom(chatId, this::onMessageReceived)

        // 구독이 제대로 되었는지 로그 확인
        android.os.Handler().postDelayed({
            Log.d(TAG, "메시지 구독 상태 확인 - 채팅방: $chatId")
        }, 5000) // 5초 후
    }

    private fun onMessageReceived(message: Message) {
        try {
            Log.d(TAG, "메시지 수신: id=${message.id}, 내용=${message.content}, 발신자=${message.senderId}")

            val currentList = _messages.value.orEmpty().toMutableList()

            // 중복 체크
            val existingMessage = currentList.find { it.id == message.id }
            if (existingMessage != null) {
                Log.d(TAG, "중복 메시지 무시: ${message.id}")
                return  // 일반 함수에서는 단순히 return 사용 가능
            }

            // 임시 메시지가 있는지 확인하고 대체
            val tempMessageIndex = currentList.indexOfFirst {
                it.content == message.content && it.id.startsWith("temp_")
            }

            if (tempMessageIndex >= 0) {
                // 임시 메시지를 실제 메시지로 교체
                Log.d(TAG, "임시 메시지 교체: ${currentList[tempMessageIndex].id} -> ${message.id}")
                currentList[tempMessageIndex] = message
            } else {
                // 신규 메시지 추가
                Log.d(TAG, "새 메시지 추가: ${message.id}")
                currentList.add(message)
            }

            // UI 업데이트
            _messages.postValue(currentList.sortedBy { it.timestamp })
        } catch (e: Exception) {
            Log.e(TAG, "메시지 처리 중 오류: ${e.message}", e)
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val messageList = repository.getMessages(chatId)
                Log.d(TAG, "메시지 로드 완료: ${messageList.size}개")

                // 시간순으로 정렬 (오래된 메시지가 먼저 오도록)
                _messages.value = messageList.sortedBy { it.timestamp }

                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e(TAG, "메시지 로드 실패: ${e.message}")
                _errorMessage.value = "메시지를 불러오는데 실패했습니다: ${e.message}"
                _messages.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 더 많은 메시지 로드 (페이징)
    fun loadMoreMessages() {
        val currentMessages = _messages.value ?: return
        if (currentMessages.isEmpty()) return

        val oldestMessageId = currentMessages.minByOrNull { it.timestamp }?.id?.toLongOrNull()

        viewModelScope.launch {
            try {
                val olderMessages = repository.getMessages(chatId, oldestMessageId)
                val allMessages = (olderMessages + currentMessages).distinctBy { it.id }
                _messages.value = allMessages.sortedBy { it.timestamp }
            } catch (e: Exception) {
                _errorMessage.value = "이전 메시지를 불러오는데 실패했습니다: ${e.message}"
            }
        }
    }

    // 메시지 전송 (WebSocket으로 직접 전송 + 임시 메시지 즉시 표시)
    fun sendMessage(content: String) {
        if (content.isBlank()) return

        try {
            // 사용자 ID 가져오기
            val userId = preferencesUtil.getUserId()?.toString() ?: "-1"
            val userEmail = preferencesUtil.getString("user_email") ?: "나"

            Log.d(TAG, "메시지 전송 시작 - 유저ID: $userId, 이메일: $userEmail, 내용: $content")

            // 임시 메시지 ID 생성
            val tempId = "temp_" + System.currentTimeMillis()

            // 즉시 UI 업데이트를 위한 임시 메시지
            val tempMessage = Message(
                id = tempId,
                roomId = chatId,
                senderId = userId,
                senderName = userEmail.split("@").firstOrNull() ?: "나",
                content = content,
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT,
                isRead = true
            )

            // 현재 메시지 목록에 임시 메시지 추가
            val currentMessages = _messages.value.orEmpty().toMutableList()
            currentMessages.add(tempMessage)
            _messages.value = currentMessages.sortedBy { it.timestamp }

            Log.d(TAG, "임시 메시지 추가: $tempId")

            // 실제 메시지 전송
            repository.sendMessage(chatId, content)
        } catch (e: Exception) {
            Log.e(TAG, "메시지 전송 중 오류: ${e.message}", e)
            _errorMessage.value = "메시지 전송에 실패했습니다: ${e.message}"
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // 메시지 읽음 표시
    fun markMessageAsRead(messageId: String) {
        repository.markMessageAsRead(chatId, messageId)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel 해제 - 구독 해제")
        // 뷰모델 제거 시 구독 해제 - 함수 참조 사용
        repository.unsubscribeFromChatRoom(chatId, this::onMessageReceived)
    }
}

class ChatRoomViewModelFactory(
    private val chatId: String,
    private val repository: ChatRoomRepository,
    private val preferencesUtil: PreferencesUtil
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatRoomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatRoomViewModel(chatId, repository, preferencesUtil) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}