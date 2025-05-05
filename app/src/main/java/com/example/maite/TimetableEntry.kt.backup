package com.example.maite.model

data class TimetableEntry(
    val id: Long? = null,    // Event ID 추가
    val title: String,
    val dayOfWeek: Int,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val colorHex: String,
    val location: String = ""
) {
    // 기존 생성자 호환성을 위한 보조 생성자
    constructor(
        title: String,
        dayOfWeek: Int,
        startHour: Int,
        endHour: Int,
        colorHex: String,
        location: String = ""
    ) : this(null, title, dayOfWeek, startHour, 0, endHour, 0, colorHex, location)
}