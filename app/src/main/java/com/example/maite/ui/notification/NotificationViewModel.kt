package com.example.maite.ui.notification

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maite.data.model.NotificationItem
import com.example.maite.data.repository.NotificationRepository
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {
    
    private val TAG = "NotificationViewModel"
    
    private val _notifications = MutableLiveData<List<NotificationItem>>(emptyList())
    val notifications: LiveData<List<NotificationItem>> = _notifications
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error
    
    init {
        loadNotifications()
    }
    
    /**
     * 모든 알림 불러오기
     */
    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d(TAG, "알림 목록 가져오기 시작")
                val result = repository.getAllNotifications()
                
                result.onSuccess { notificationList ->
                    Log.d(TAG, "알림 목록 로드 성공: ${notificationList.size}개")
                    _notifications.value = notificationList
                }.onFailure { e ->
                    Log.e(TAG, "알림 목록 로드 실패", e)
                    _error.value = "알림을 불러오는데 실패했습니다."
                }
            } catch (e: Exception) {
                Log.e(TAG, "알림 목록 로드 중 예외 발생", e)
                _error.value = "알림을 불러오는 중 오류가 발생했습니다."
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 새로고침
     */
    fun refresh() {
        loadNotifications()
    }
    
    /**
     * 에러 초기화
     */
    fun clearError() {
        _error.value = null
    }
}
