package com.example.maite.model

data class Event(
    val id: Long = 0,
    val title: String,
    val day: String,         // 예: "MONDAY"
    val place: String,       // 예: "강의실 A"
    val startTime: String,   // 예: "09:00"
    val endTime: String      // 예: "10:30"
)
