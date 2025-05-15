package com.example.maite

data class RoomTimetableResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: TimetableResult
)

data class TimetableResult(
    val timetableId: Long,
    val userId: Long,
    val userName: String,
    val mateCount: Int,
    val events: List<TimetableEvent>
)

data class TimetableEvent(
    val id: Long,
    val title: String,
    val day: String,  // 요일 (예: "MON", "TUE" 등)
    val place: String,
    val startTime: String,  // 시작 시간 (예: "09:00")
    val endTime: String     // 종료 시간 (예: "11:30")
)