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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PropMeetViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PropMeetRepository.getInstance()
    private val apiService = MaiteRetrofitClient.getInstance(application)

    private val _proposedMeetings = MutableLiveData<List<PropMeetItem>>()
    val proposedMeetings: LiveData<List<PropMeetItem>> = _proposedMeetings

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // 날짜 형식 파서 (yyyy-MM-dd)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        loadProposedMeetings()
    }

    fun loadProposedMeetings() {
        Log.d("PropMeetViewModel", "loadProposedMeetings 호출")
        val meetings = repository.getProposedMeetings()

        // 날짜 기준으로 정렬 (제일 빠른 날짜가 맨 위에)
        val sortedMeetings = meetings.sortedBy { meetItem ->
            try {
                // 날짜 파싱
                dateFormat.parse(meetItem.date) ?: Date(Long.MAX_VALUE)
            } catch (e: Exception) {
                Log.e("PropMeetViewModel", "날짜 파싱 오류: ${meetItem.date}", e)
                // 파싱 오류 시 가장 나중 날짜로 처리
                Date(Long.MAX_VALUE)
            }
        }

        Log.d("PropMeetViewModel", "회의 정렬 완료: ${sortedMeetings.size}개 항목")
        sortedMeetings.forEachIndexed { index, item ->
            Log.d("PropMeetViewModel", "$index: ${item.title}, 날짜: ${item.date}")
        }

        _proposedMeetings.value = sortedMeetings
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