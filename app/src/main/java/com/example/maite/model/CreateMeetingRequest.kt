package com.example.maite.model

data class CreateMeetingRequest(
    val title: String,
    val meetingDate: String,
    val meetingTime: String,
    val meetingEndTime: String,
    val inviteEmails: List<String>
)