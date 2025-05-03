package com.example.maite.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.maite.data.TimetableStore
import com.example.maite.data.model.MeetingItem
import com.example.maite.data.model.MeetingProposal
import com.example.maite.model.TimetableEntry

class HomeViewModel : ViewModel() {

    val timetableEntries: LiveData<List<TimetableEntry>> = TimetableStore.entries

    private val _nearestMeeting = MutableLiveData<MeetingItem?>()
    val nearestMeeting: LiveData<MeetingItem?> = _nearestMeeting

    private val _proposals = MutableLiveData<List<MeetingProposal>>()
    val proposals: LiveData<List<MeetingProposal>> = _proposals

    private val acceptedMeetings = mutableListOf<MeetingItem>()

    init {
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
    }

    fun acceptProposal(proposal: MeetingProposal) {
        _proposals.value = _proposals.value?.filterNot { it.id == proposal.id }

        val meeting = MeetingItem(
            id = proposal.id,
            title = proposal.title,
            date = proposal.date,
            startTime = proposal.time.split("~").first().trim(),
            endTime = proposal.time.split("~").last().trim(),
            location = proposal.location
        )
        acceptedMeetings.add(meeting)
        _nearestMeeting.value = acceptedMeetings.firstOrNull()
    }

    fun declineProposal(proposal: MeetingProposal) {
        _proposals.value = _proposals.value?.filterNot { it.id == proposal.id }
    }

    fun getAcceptedMeetings(): List<MeetingItem> = acceptedMeetings
}
