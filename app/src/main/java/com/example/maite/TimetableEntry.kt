package com.example.maite.model

data class TimetableEntry(
    val title: String,
    val dayOfWeek: Int,   // 1:월, 2:화, ..., 7:일
    val startHour: Int,   // 예: 10
    val endHour: Int,     // 예: 12
    val colorHex: String, // "#A5BEF5" 형식
    val location: String = "" // 장소 정보 추가
)