package com.example.maite.util.mapper

import com.example.maite.model.TimetableEntry
import com.example.maite.model.Event
import com.example.maite.model.EventRequest

// TimetableEntry → 서버 전송용 EventRequest로 변환
fun TimetableEntry.toEventRequest(): EventRequest {
    return EventRequest(
        title = this.title,
        day = this.dayOfWeek.toServerDayString(),
        place = this.location,
        startTime = String.format("%02d:%02d", startHour, startMinute),
        endTime = String.format("%02d:%02d", endHour, endMinute)
    )
}

// TimetableEntry → 서버 응답용 Event로 변환 (서버에서 받은 데이터를 다시 가공할 때 사용)
fun TimetableEntry.toEvent(): Event {
    return Event(
        id = 0, // 서버에서 받은 데이터로 교체 필요
        title = this.title,
        day = this.dayOfWeek.toServerDayString(),
        place = this.location,
        startTime = String.format("%02d:%02d", startHour, startMinute),
        endTime = String.format("%02d:%02d", endHour, endMinute)
    )
}

// 🆕 서버에서 받은 Event → TimetableEntry 로 변환
fun Event.toTimetableEntry(): TimetableEntry {
    val (startH, startM) = this.startTime.split(":").map { it.toInt() }
    val (endH, endM) = this.endTime.split(":").map { it.toInt() }

    return TimetableEntry(
        title = this.title,
        dayOfWeek = this.day.toDayOfWeek(),
        startHour = startH,
        startMinute = startM,
        endHour = endH,
        endMinute = endM,
        colorHex = "#4C7EED", // 서버에는 색상이 없기 때문에 기본 색상 부여
        location = this.place
    )
}

// 요일 숫자 → 서버 문자열 변환
fun Int.toServerDayString(): String {
    return when (this) {
        1 -> "MONDAY"
        2 -> "TUESDAY"
        3 -> "WEDNESDAY"
        4 -> "THURSDAY"
        5 -> "FRIDAY"
        6 -> "SATURDAY"
        7 -> "SUNDAY"
        else -> throw IllegalArgumentException("Invalid day index: $this")
    }
}

// 서버 문자열 → 요일 숫자 변환 (Event → TimetableEntry 변환 시 사용)
fun String.toDayOfWeek(): Int {
    return when (this.uppercase()) {
        "MONDAY" -> 1
        "TUESDAY" -> 2
        "WEDNESDAY" -> 3
        "THURSDAY" -> 4
        "FRIDAY" -> 5
        "SATURDAY" -> 6
        "SUNDAY" -> 7
        else -> throw IllegalArgumentException("Invalid day string: $this")
    }
}
