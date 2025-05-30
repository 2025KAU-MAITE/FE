package com.example.maite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.maite.model.MeetingDataManager
import com.example.maite.model.PropMeetItem
import com.example.maite.model.PropMeetRepository

// Application 컨텍스트를 사용하기 위해 AndroidViewModel로 변경
class PropMeetViewModel(application: Application) : AndroidViewModel(application) {

    private val meetingDataManager = MeetingDataManager(application)

    // 기존 repository 대신 meetingDataManager에서 제공하는 repository 사용
    private val repository = PropMeetRepository.getInstance()

    private val _proposedMeetings = MutableLiveData<List<PropMeetItem>>()
    val proposedMeetings: LiveData<List<PropMeetItem>> = _proposedMeetings

    init {
        loadProposedMeetings()
    }

    // 제안된 회의 데이터를 로드
    fun loadProposedMeetings() {
        _proposedMeetings.value = repository.getProposedMeetings()
    }

    // 아이템 제거 함수
    private fun removeMeetingFromList(itemToRemove: PropMeetItem) {
        val currentList = _proposedMeetings.value?.toMutableList() ?: mutableListOf()
        currentList.remove(itemToRemove) // 리스트에서 해당 아이템 제거
        _proposedMeetings.value = currentList // 변경된 리스트로 LiveData 업데이트
    }

    fun acceptMeeting(item: PropMeetItem) {
        println("회의 수락: ${item.title}")
        // 실제 수락 처리 로직 (예: 서버 API 호출) 이 필요하다면 여기에 추가
        removeMeetingFromList(item) // 리스트에서 제거
    }

    fun rejectMeeting(item: PropMeetItem) {
        println("회의 거절: ${item.title}")
        // 실제 거절 처리 로직 (예: 서버 API 호출) 이 필요하다면 여기에 추가
        removeMeetingFromList(item) // 리스트에서 제거
    }

    // Factory 클래스를 내부에 정의
    class Factory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PropMeetViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PropMeetViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}