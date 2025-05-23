package com.example.maite.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.maite.model.ChatRoomRepository
import com.example.maite.model.Message
import kotlinx.coroutines.launch

class ChatRoomViewModel(
    private val chatId: String,
    private val repository: ChatRoomRepository
) : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        // 메시지만 로드 (채팅방 정보는 ChatListItem에서 이미 가져옴)
        loadMessages()
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val messageList = repository.getMessages(chatId)
                _messages.value = messageList
                _errorMessage.value = null
            } catch (e: Exception) {
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

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            try {
                val newMessage = repository.sendMessage(chatId, content)
                val currentList = _messages.value.orEmpty().toMutableList()
                currentList.add(newMessage)
                _messages.value = currentList
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "메시지 전송에 실패했습니다: ${e.message}"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

class ChatRoomViewModelFactory(
    private val chatId: String,
    private val repository: ChatRoomRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatRoomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatRoomViewModel(chatId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}