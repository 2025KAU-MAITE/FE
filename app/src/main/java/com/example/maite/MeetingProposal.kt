package com.example.maite.data.model

data class MeetingProposal(
    val id: Int,
    val title: String,
    val date: String,      // 예: "2025.04.06"
    val time: String,      // 예: "10:00 ~ 11:00"
    val location: String,
    val fromUser: String   // 예: 제안자 이름
)
