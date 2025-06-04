package com.example.maite.model

data class MeetingResponse(
    val meetingId: Long,
    val title: String?,
    val proposerName: String?,
    val meetingDate: String?,
    val meetingTime: String?,
    val meetingEndTime: String?,
    val address: String?,
    val acceptance: String?
)