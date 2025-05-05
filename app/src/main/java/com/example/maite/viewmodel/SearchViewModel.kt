package com.example.maite.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maite.model.User
import com.example.maite.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class SearchViewModel(private val userRepository: UserRepository) : ViewModel() {
    
    private val _searchResults = MutableLiveData<List<User>>()
    val searchResults: LiveData<List<User>> = _searchResults
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _friendRequestSent = MutableLiveData<Boolean>()
    val friendRequestSent: LiveData<Boolean> = _friendRequestSent
    
    fun searchUsers(query: String) {
        if (query.isEmpty()) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Use mock data for development, replace with real API for production
                // val result = userRepository.searchUsers(query)
                val mockUsers = userRepository.getMockUsers(query)
                _searchResults.value = mockUsers
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "검색 중 오류가 발생했습니다: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun sendFriendRequests(selectedUsers: List<User>) {
        if (selectedUsers.isEmpty()) {
            _errorMessage.value = "선택된 사용자가 없습니다"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // For development, just simulate success
                // val result = userRepository.sendFriendRequests(selectedUsers.map { it.id })
                // if (result.isSuccess) {
                //     _friendRequestSent.value = true
                // } else {
                //     _errorMessage.value = "친구 요청을 보내는 데 실패했습니다"
                // }
                
                // Mock success
                _friendRequestSent.value = true
            } catch (e: Exception) {
                _errorMessage.value = "요청 중 오류가 발생했습니다: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun resetFriendRequestSent() {
        _friendRequestSent.value = false
    }
}