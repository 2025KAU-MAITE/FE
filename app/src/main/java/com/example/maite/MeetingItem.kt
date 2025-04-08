package com.example.maite.data.model

data class MeetingItem(
    val id: Int,
    val title: String,
    val date: String,      // 예: "2025.04.05"
    val startTime: String, // 예: "13:00"
    val endTime: String,   // 예: "14:00"
    val location: String
)
