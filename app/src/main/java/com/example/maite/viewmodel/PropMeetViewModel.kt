package com.example.maite.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.maite.MaiteApiService
import com.example.maite.MaiteRetrofitClient
import com.example.maite.model.MeetingDataManager
import com.example.maite.model.PropMeetItem
import com.example.maite.model.PropMeetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PropMeetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PropMeetRepository.getInstance()
    private val apiService = MaiteRetrofitClient.getInstance(application)

    private val _proposedMeetings = MutableLiveData<List<PropMeetItem>>()
    val proposedMeetings: LiveData<List<PropMeetItem>> = _proposedMeetings

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadProposedMeetings()
    }

    fun loadProposedMeetings() {
        Log.d("PropMeetViewModel", "loadProposedMeetings 호출")
        val meetings = repository.getProposedMeetings()
        _proposedMeetings.value = meetings
    }

    fun acceptMeeting(item: PropMeetItem) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.acceptMeetingInvite(item.meetingId)
                }

                if (response.isSuccessful) {
                    Log.d("PropMeetViewModel", "회의 수락 성공: 회의 ID ${item.meetingId}")

                    // 로컬 저장소에서 항목 업데이트
                    val updatedItem = item.copy(acceptance = "ACCEPTED")
                    repository.updateProposal(updatedItem)

                    // LiveData 업데이트
                    loadProposedMeetings()
                } else {
                    Log.e("PropMeetViewModel", "회의 수락 실패: ${response.code()}")
                    _error.value = "회의 수락 실패: 상태 코드 ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("PropMeetViewModel", "회의 수락 처리 중 오류", e)
                _error.value = "회의 수락 처리 중 오류: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rejectMeeting(item: PropMeetItem) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.rejectMeetingInvite(item.meetingId)
                }

                if (response.isSuccessful) {
                    Log.d("PropMeetViewModel", "회의 거절 성공: 회의 ID ${item.meetingId}")

                    // 로컬 저장소에서 항목 업데이트
                    val updatedItem = item.copy(acceptance = "REJECTED")
                    repository.updateProposal(updatedItem)

                    // LiveData 업데이트
                    loadProposedMeetings()
                } else {
                    Log.e("PropMeetViewModel", "회의 거절 실패: ${response.code()}")
                    _error.value = "회의 거절 실패: 상태 코드 ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("PropMeetViewModel", "회의 거절 처리 중 오류", e)
                _error.value = "회의 거절 처리 중 오류: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PropMeetViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PropMeetViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}