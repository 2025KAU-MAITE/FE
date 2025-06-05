package com.example.maite.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.maite.MaiteRetrofitClient
import com.example.maite.model.ChatListItem
import com.example.maite.model.ChatListRepository
import kotlinx.coroutines.launch

class ChatListViewModel(val repository: ChatListRepository) : ViewModel() {

    // 현재 표시할 채팅 목록
    private val _chatItems = MutableLiveData<List<ChatListItem>>()
    val chatItems: LiveData<List<ChatListItem>> = _chatItems

    // 로딩 상태
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // 에러 메시지
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // 현재 선택된 탭 (개인/단체)
    private val _isPersonalTab = MutableLiveData<Boolean>(true)
    val isPersonalTab: LiveData<Boolean> = _isPersonalTab

    init {
        // 초기 로드는 개인 채팅 목록
        loadChatList(true)
    }

    // 채팅 목록 로드
    fun loadChatList(isPersonal: Boolean) {
        _isLoading.value = true
        _isPersonalTab.value = isPersonal

        viewModelScope.launch {
            try {
                val chatList = if (isPersonal) {
                    repository.getPersonalChats()
                } else {
                    repository.getGroupChats()
                }
                _chatItems.value = chatList
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "채팅 목록을 불러오는 데 실패했습니다: ${e.message}"
                _chatItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 채팅 검색
    fun searchChats(query: String) {
        if (query.isEmpty()) {
            loadChatList(_isPersonalTab.value ?: true)
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val results = repository.searchChats(query, _isPersonalTab.value ?: true)
                _chatItems.value = results
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "검색 중 오류가 발생했습니다: ${e.message}"
                _chatItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 사용자 검색 함수 추가
    fun searchUsers(query: String) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val apiService = MaiteRetrofitClient.getInstance(repository.getContext())
                val response = apiService.searchUsers(query)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val users = response.body()?.result ?: emptyList()

                    // 검색된 사용자 정보를 ChatListItem으로 변환
                    val userItems = users.map { user ->
                        ChatListItem(
                            id = user.id.toString(),
                            name = user.name,
                            profileImageUrl = user.profileImageUrl,
                            lastMessage = null,
                            intro = user.email,  // 이메일을 intro 필드에 표시
                            timestamp = null,
                            isGroup = false,
                            isUser = true  // 사용자 아이템임을 표시
                        )
                    }

                    _chatItems.value = userItems
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "사용자 검색에 실패했습니다: ${response.body()?.message ?: "서버 오류"}"
                    _chatItems.value = emptyList()
                }
            } catch (e: Exception) {
                _errorMessage.value = "검색 중 오류가 발생했습니다: ${e.message}"
                _chatItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 채팅 목록 새로고침
    fun refreshChatList() {
        loadChatList(_isPersonalTab.value ?: true)
    }

    // 에러 메시지 초기화
    fun clearError() {
        _errorMessage.value = null
    }
}

// ViewModel Factory
class ChatListViewModelFactory(private val repository: ChatListRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}