package com.example.maite.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.maite.model.ChatRoom
import com.example.maite.model.ChatRoomRepository
import com.example.maite.model.Message
import kotlinx.coroutines.launch

class ChatRoomViewModel(
    private val chatId: String,
    private val repository: ChatRoomRepository
) : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = _messages

    private val _chatRoom = MutableLiveData<ChatRoom>()
    val chatRoom: LiveData<ChatRoom> = _chatRoom

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadChatRoom()
        loadMessages()
    }

    private fun loadChatRoom() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val room = repository.getChatRoomInfo(chatId)
                _chatRoom.value = room
            } catch (e: Exception) {
                // 에러 처리
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val messageList = repository.getMessages(chatId)
                _messages.value = messageList
            } catch (e: Exception) {
                // 에러 처리
            } finally {
                _isLoading.value = false
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
            } catch (e: Exception) {
                // 에러 처리
            }
        }
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