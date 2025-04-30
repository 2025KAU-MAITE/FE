package com.example.maite.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maite.data.TimetableDataHolder
import com.example.maite.data.model.MeetingItem
import com.example.maite.data.model.MeetingProposal
import com.example.maite.model.TimetableEntry
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class HomeViewModel : ViewModel() {

    // 내 시간표
    private val _timetableEntries = MutableLiveData<List<TimetableEntry>>(emptyList())
    val timetableEntries: LiveData<List<TimetableEntry>> = _timetableEntries

    // 가장 가까운 회의
    private val _nearestMeeting = MutableLiveData<MeetingItem?>()
    val nearestMeeting: LiveData<MeetingItem?> = _nearestMeeting

    // 제안 목록
    private val _proposals = MutableLiveData<List<MeetingProposal>>()
    val proposals: LiveData<List<MeetingProposal>> = _proposals

    // 수락된 회의 목록 (상태 유지용)
    private val acceptedMeetings = mutableListOf<MeetingItem>()

    init {
        // 초기 더미 데이터 (서버 연동 전)
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

        _nearestMeeting.value = null

        // TimetableDataHolder에서 데이터 변경 감지
        TimetableDataHolder.timetableEntries
            .onEach { entries ->
                _timetableEntries.value = entries
            }
            .launchIn(viewModelScope)
    }

    fun acceptProposal(proposal: MeetingProposal) {
        // 제안 리스트에서 제거
        _proposals.value = _proposals.value?.filterNot { it.id == proposal.id }

        // 수락된 회의로 변환해서 저장
        val meeting = MeetingItem(
            id = proposal.id,
            title = proposal.title,
            date = proposal.date,
            startTime = proposal.time.split("~").first().trim(),
            endTime = proposal.time.split("~").last().trim(),
            location = proposal.location
        )
        acceptedMeetings.add(meeting)

        // 가장 가까운 회의로 설정
        _nearestMeeting.value = acceptedMeetings.firstOrNull()
    }

    fun declineProposal(proposal: MeetingProposal) {
        _proposals.value = _proposals.value?.filterNot { it.id == proposal.id }
        // 거절은 상태 저장 안 함 (필요 시 추가 가능)
    }

    // 다른 화면 갔다와도 상태 유지
    fun getAcceptedMeetings(): List<MeetingItem> {
        return acceptedMeetings
    }
}