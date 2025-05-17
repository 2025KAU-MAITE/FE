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
            if (dayOfWeek == 0) return@forEach // Skip if day is unknown

            val startHour = timeStringToHour(event.startTime)
            val endHour = timeStringToHour(event.endTime)

            // 각 시간대를 바쁜 시간으로 표시
            for (hour in startHour until endHour) {
                if (hour in 0..23) { // Ensure hour is within valid range
                    busyHours[dayOfWeek]?.add(hour)
                }
            }

            Log.d("RoomTimetableUtils", "바쁜 시간 추가: ${event.title}, 요일=${event.day}(${dayOfWeek}), 시간=${startHour}-${endHour}")
        }

        return busyHours
    }

    // 각 시간대별로 몇 명이 바쁜지 계산하는 함수
    fun calculateBusyCountsPerSlot(
        allUsersBusyHours: List<Map<Int, Set<Int>>>
    ): Map<Int, Map<Int, Int>> { // Day -> (Hour -> Busy Count)
        val busyCounts = mutableMapOf<Int, MutableMap<Int, Int>>()

        // 모든 요일과 시간에 대해 바쁜 사람 수를 0으로 초기화
        for (day in 1..7) {
            busyCounts[day] = mutableMapOf()
            for (hour in 0..23) {
                busyCounts[day]!![hour] = 0
            }
        }

        // 각 사용자의 바쁜 시간을 순회하며 카운트 증가
        for (userBusyHours in allUsersBusyHours) {
            for (day in 1..7) {
                userBusyHours[day]?.forEach { busyHour ->
                    if (busyHour in 0..23) { // Ensure hour is valid
                        busyCounts[day]!![busyHour] = (busyCounts[day]!![busyHour] ?: 0) + 1
                    }
                }
            }
        }
        Log.d("RoomTimetableUtils", "시간대별 바쁜 사용자 수 계산 완료: $busyCounts")
        return busyCounts
    }

    // 계산된 바쁜 사용자 수를 기반으로 TimetableItem 리스트 생성
    fun convertBusyCountsToTimetableItems(
        busyCountsPerSlot: Map<Int, Map<Int, Int>>,
        totalParticipants: Int,
        allFreeColor: Int,
        nMinusOneFreeColor: Int
    ): List<ListDetailFragment.TimetableItem> {
        val result = mutableListOf<ListDetailFragment.TimetableItem>()

        if (totalParticipants == 0) {
            Log.d("RoomTimetableUtils", "참가자가 없습니다. 시간표 항목을 생성하지 않습니다.")
            return result
        }

        for (day in 1..7) {
            for (hour in 0..23) {
                val busyCount = busyCountsPerSlot[day]?.get(hour) ?: 0

                if (busyCount == 0) { // N명 모두 비는 경우
                    result.add(
                        ListDetailFragment.TimetableItem(
                            timeSlot = hour,
                            dayOfWeek = day,
                            className = "모두 비는 시간 (${totalParticipants}명)",
                            color = allFreeColor
                        )
                    )
                } else if (totalParticipants > 1 && busyCount == 1) { // N-1명이 비는 경우 (즉, 1명만 바쁨)
                    result.add(
                        ListDetailFragment.TimetableItem(
                            timeSlot = hour,
                            dayOfWeek = day,
                            className = "${totalParticipants - 1}명 비는 시간",
                            color = nMinusOneFreeColor
                        )
                    )
                }
            }
        }
        Log.d("RoomTimetableUtils", "총 ${result.size}개의 (N명 또는 N-1명) 비는 시간 항목 생성됨 (총 참가자: $totalParticipants)")
        return result
    }


    // 기존 함수들은 유지하되, 위의 새로운 함수들이 주 로직을 담당합니다.
    // 모든 참가자의 바쁜 시간 정보를 병합하여 공통으로 비는 시간을 찾는 함수 (기존 로직)
    fun combineAllUserBusyHours(allUsersBusyHours: List<Map<Int, Set<Int>>>): Map<Int, Set<Int>> {
        val combinedBusyHours = mutableMapOf<Int, MutableSet<Int>>()
        for (day in 1..7) {
            combinedBusyHours[day] = mutableSetOf()
        }
        for (userBusyHours in allUsersBusyHours) {
            for (day in 1..7) {
                val userBusyHoursForDay = userBusyHours[day] ?: emptySet()
                combinedBusyHours[day]?.addAll(userBusyHoursForDay)
            }
        }
        return combinedBusyHours
    }

    // 병합된 바쁜 시간을 기반으로 모두가 비는 시간을 TimetableItem 리스트로 변환 (기존 로직)
    fun convertCombinedBusyHoursToFreeTimetableItems(
        combinedBusyHours: Map<Int, Set<Int>>,
        freeTimeColor: Int
    ): List<ListDetailFragment.TimetableItem> {
        val result = mutableListOf<ListDetailFragment.TimetableItem>()
        for (day in 1..7) {
            for (hour in 0..23) {
                if (!(combinedBusyHours[day]?.contains(hour) == true)) {
                    result.add(
                        ListDetailFragment.TimetableItem(
                            timeSlot = hour,
                            dayOfWeek = day,
                            className = "모두 비는 시간",
                            color = freeTimeColor
                        )
                    )
                }
            }
        }
        return result
    }
}