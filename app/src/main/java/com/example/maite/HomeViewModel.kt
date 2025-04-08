package com.example.maite.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.maite.data.model.MeetingItem
import com.example.maite.data.model.MeetingProposal

class HomeViewModel : ViewModel() {

    // 가장 가까운 회의 (내 회의)
    private val _nearestMeeting = MutableLiveData<MeetingItem?>()
    val nearestMeeting: LiveData<MeetingItem?> = _nearestMeeting

    // 읽지 않은 회의 제안 리스트
    private val _proposals = MutableLiveData<List<MeetingProposal>>()
    val proposals: LiveData<List<MeetingProposal>> = _proposals

    init {
        loadDummyData()
    }

    private fun loadDummyData() {
        // 기본적으로는 회의 제안만 있고, 내 회의는 없는 상태
        _nearestMeeting.value = null

        _proposals.value = listOf(
            MeetingProposal(
                id = 101,
                title = "기획 회의",
                date = "2025.04.05",
                time = "13:00 ~ 14:00",
                location = "서울 마포구 홍대입구",
                fromUser = "문정우"
            )
        )
    }

    fun acceptProposal(proposal: MeetingProposal) {
        // 1. 내 회의로 등록
        _nearestMeeting.value = MeetingItem(
            id = proposal.id,
            title = proposal.title,
            date = proposal.date,
            startTime = proposal.time.split("~")[0].trim(),
            endTime = proposal.time.split("~")[1].trim(),
            location = proposal.location
        )

        // 2. 제안 목록에서 제거
        _proposals.value = _proposals.value?.filterNot { it.id == proposal.id }
    }

    fun declineProposal(proposal: MeetingProposal) {
        // 제안 목록에서 해당 제안 제거
        _proposals.value = _proposals.value?.filterNot { it.id == proposal.id }
    }
}
