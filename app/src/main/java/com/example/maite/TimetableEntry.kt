package com.example.maite.model

data class TimetableEntry(
    val title: String,
    val dayOfWeek: Int,      // 1:월, 2:화, ..., 7:일
    val startHour: Int,      // 시작 시간 (시)
    val startMinute: Int,    // 시작 시간 (분) - 0 또는 30
    val endHour: Int,        // 종료 시간 (시)
    val endMinute: Int,      // 종료 시간 (분) - 0 또는 30
    val colorHex: String,    // "#A5BEF5" 형식
    val location: String = "" // 장소 정보
) {
    // 보조 생성자 (분 정보가 없는 경우 기본값 0으로 설정)
    constructor(
        title: String,
        dayOfWeek: Int,
        startHour: Int,
        endHour: Int,
        colorHex: String,
        location: String = ""
    ) : this(title, dayOfWeek, startHour, 0, endHour, 0, colorHex, location)
}
