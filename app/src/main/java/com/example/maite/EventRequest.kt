package com.example.maite.model

data class EventRequest(
    val title: String,
    val day: String,
    val place: String,
    val startTime: String,  // "HH:mm"
    val endTime: String     // "HH:mm"
)
