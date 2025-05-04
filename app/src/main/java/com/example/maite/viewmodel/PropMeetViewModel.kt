package com.example.maite.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.maite.model.PropMeetItem
import com.example.maite.model.PropMeetRepository

class PropMeetViewModel : ViewModel() {

    private val repository = PropMeetRepository()

    private val _proposedMeetings = MutableLiveData<List<PropMeetItem>>()
    val proposedMeetings: LiveData<List<PropMeetItem>> = _proposedMeetings

    init {
        loadProposedMeetings()
    }

    private fun loadProposedMeetings() {
        _proposedMeetings.value = repository.getProposedMeetings()
    }

    // 아이템 제거 함수 추가
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
}