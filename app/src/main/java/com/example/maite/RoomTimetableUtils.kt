package com.example.maite

import android.graphics.Color
import android.util.Log

object RoomTimetableUtils {
    // 요일 문자열을 숫자로 변환 (월: 1, 화: 2, ...)
    fun dayStringToInt(day: String): Int {
        return when (day.lowercase()) {
            "mon", "monday", "월" -> 1
            "tue", "tuesday", "화" -> 2
            "wed", "wednesday", "수" -> 3
            "thu", "thursday", "목" -> 4
            "fri", "friday", "금" -> 5
            "sat", "saturday", "토" -> 6
            "sun", "sunday", "일" -> 7
            else -> {
                Log.e("RoomTimetableUtils", "알 수 없는 요일 형식: $day")
                0
            }
        }
    }

    // 시간 문자열을 정수로 변환 (예: "09:30" -> 9)
    fun timeStringToHour(time: String): Int {
        return time.split(":").first().toIntOrNull() ?: 0
    }

    // 단일 사용자의 API 응답을 바쁜 시간 맵으로 변환
    fun convertToUserBusyHours(response: RoomTimetableResponse): Map<Int, Set<Int>> {
        // 바쁜 시간대를 저장할 맵 (요일 -> 바쁜 시간대 집합)
        val busyHours = mutableMapOf<Int, MutableSet<Int>>()

        // 모든 요일에 대해 빈 집합 초기화
        for (day in 1..7) {
            busyHours[day] = mutableSetOf()
        }

        // API 응답에서 바쁜 시간대 채우기
        response.result.events.forEach { event ->
            val dayOfWeek = dayStringToInt(event.day)
            val startHour = timeStringToHour(event.startTime)
            val endHour = timeStringToHour(event.endTime)

            // 각 시간대를 바쁜 시간으로 표시
            for (hour in startHour until endHour) {
                busyHours[dayOfWeek]?.add(hour)
            }

            Log.d("RoomTimetableUtils", "바쁜 시간 추가: ${event.title}, 요일=${event.day}(${dayOfWeek}), 시간=${startHour}-${endHour}")
        }

        return busyHours
    }

    // 모든 참가자의 바쁜 시간 정보를 병합하여 공통으로 비는 시간을 찾는 함수
    fun combineAllUserBusyHours(allUsersBusyHours: List<Map<Int, Set<Int>>>): Map<Int, Set<Int>> {
        // 모든 사용자의 바쁜 시간을 병합할 결과 맵
        val combinedBusyHours = mutableMapOf<Int, MutableSet<Int>>()

        // 모든 요일에 대해 빈 집합 초기화
        for (day in 1..7) {
            combinedBusyHours[day] = mutableSetOf()
        }

        // 모든 사용자의 바쁜 시간 병합
        for (userBusyHours in allUsersBusyHours) {
            for (day in 1..7) {
                val userBusyHoursForDay = userBusyHours[day] ?: emptySet()
                combinedBusyHours[day]?.addAll(userBusyHoursForDay)
            }
        }

        return combinedBusyHours
    }

    // 병합된 바쁜 시간을 기반으로 모두가 비는 시간을 TimetableItem 리스트로 변환
    fun convertCombinedBusyHoursToFreeTimetableItems(
        combinedBusyHours: Map<Int, Set<Int>>,
        freeTimeColor: Int
    ): List<ListDetailFragment.TimetableItem> {
        val result = mutableListOf<ListDetailFragment.TimetableItem>()

        // 모든 요일과 시간대에 대해 비는 시간만 TimetableItem으로 추가
        for (day in 1..7) {
            for (hour in 0..23) {
                // 해당 시간이 바쁜 시간대 집합에 없으면 비는 시간
                if (!(combinedBusyHours[day]?.contains(hour) == true)) {
                    result.add(
                        ListDetailFragment.TimetableItem(
                            timeSlot = hour,
                            dayOfWeek = day,
                            className = "모두 비는 시간",
                            color = freeTimeColor
                        )
                    )
                    Log.d("RoomTimetableUtils", "비는 시간 추가: 요일=${day}, 시간=${hour}")
                }
            }
        }

        Log.d("RoomTimetableUtils", "총 비는 시간 항목 수: ${result.size}")
        return result
    }
}