package com.example.maite.repository

import android.content.Context
import com.example.maite.ApiClient
import com.example.maite.api.*
import com.example.maite.model.TimetableEntry
import com.example.maite.PreferencesUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class TimetableRepository(private val context: Context) {

    private val TAG = "TimetableRepository"
    private val timetableApi = ApiClient.getClient(context).create(TimetableApi::class.java)
    private val preferencesUtil = PreferencesUtil(context)

    // 사용자의 시간표 ID를 SharedPreferences에 저장
    private fun saveTimetableId(userId: Long, timetableId: Long) {
        preferencesUtil.saveLong("timetable_id_$userId", timetableId)
    }

    private fun getTimetableId(userId: Long): Long? {
        return preferencesUtil.getLong("timetable_id_$userId")
    }

    suspend fun createOrGetTimetable(userId: Long): Long? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "createOrGetTimetable called with userId: $userId")
                
                // 기존에 저장된 timetableId가 있는지 확인
                val savedTimetableId = getTimetableId(userId)
                Log.d(TAG, "Saved timetableId: $savedTimetableId")
                
                if (savedTimetableId != null) {
                    // 유효한지 확인 (GET 요청으로)
                    try {
                        val response = timetableApi.getTimetable(savedTimetableId)
                        Log.d(TAG, "Checking existing timetable: isSuccessful=${response.isSuccessful}, code=${response.code()}")
                        
                        if (response.isSuccessful && response.body()?.isSuccess == true) {
                            Log.d(TAG, "Using existing timetableId: $savedTimetableId")
                            return@withContext savedTimetableId
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking existing timetable", e)
                    }
                }

                // 없거나 유효하지 않으면 새로 생성
                Log.d(TAG, "Creating new timetable for userId: $userId")
                val request = CreateTimetableRequest(title = "My Timetable", userId = userId)
                val response = timetableApi.createTimetable(request)
                
                Log.d(TAG, "Create timetable response: isSuccessful=${response.isSuccessful}, code=${response.code()}")

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val timetableId = response.body()?.result?.id
                    Log.d(TAG, "Created new timetableId: $timetableId")
                    
                    if (timetableId != null) {
                        saveTimetableId(userId, timetableId)
                    }
                    timetableId
                } else {
                    Log.e(TAG, "Failed to create timetable: ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "시간표 생성/조회 중 오류 발생", e)
                null
            }
        }
    }

    suspend fun saveTimetable(userId: Long, entries: List<TimetableEntry>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 시간표 ID 가져오기 또는 생성
                val timetableId = createOrGetTimetable(userId)
                if (timetableId == null) {
                    Log.e(TAG, "시간표 ID를 가져올 수 없습니다")
                    return@withContext false
                }

                // 기존 이벤트 모두 삭제 - 수정됨
                val success = deleteAllEvents(timetableId)
                if (!success) {
                    Log.e(TAG, "기존 이벤트 삭제 실패")
                    return@withContext false
                }

                // 각 이벤트를 서버에 저장
                var savingSuccess = true
                entries.forEach { entry ->
                    val eventRequest = CreateEventRequest(
                        title = entry.title,
                        day = getDayString(entry.dayOfWeek),
                        color = entry.colorHex,
                        startTime = String.format("%02d:%02d", entry.startHour, entry.startMinute),
                        endTime = String.format("%02d:%02d", entry.endHour, entry.endMinute)
                    )

                    val response = timetableApi.createEvent(timetableId, eventRequest)
                    if (!response.isSuccessful) {
                        Log.e(TAG, "이벤트 저장 실패: ${entry.title}")
                        savingSuccess = false
                    }
                }

                savingSuccess
            } catch (e: Exception) {
                Log.e(TAG, "시간표 저장 중 오류 발생", e)
                false
            }
        }
    }

    suspend fun loadTimetable(userId: Long): List<TimetableEntry> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Loading timetable for userId: $userId")
                
                val timetableId = createOrGetTimetable(userId)
                if (timetableId == null) {
                    Log.e(TAG, "시간표 ID를 가져올 수 없습니다")
                    return@withContext emptyList()
                }
                
                Log.d(TAG, "Got timetableId: $timetableId")

                val response = timetableApi.getAllEvents(timetableId)
                Log.d(TAG, "API response: isSuccessful=${response.isSuccessful}, code=${response.code()}")

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val events = response.body()?.result ?: emptyList()
                    Log.d(TAG, "Loaded ${events.size} events from server")
                    
                    val timetableEntries = events.map { eventDto ->
                        Log.d(TAG, "Converting event: ${eventDto.title}, ${eventDto.day}, ${eventDto.startTime} - ${eventDto.endTime}")
                        convertEventDtoToTimetableEntry(eventDto)
                    }
                    
                    Log.d(TAG, "Converted to ${timetableEntries.size} timetable entries")
                    timetableEntries
                } else {
                    Log.e(TAG, "Failed to load events: ${response.errorBody()?.string()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "시간표 로드 중 오류 발생", e)
                emptyList()
            }
        }
    }

    private fun getDayString(dayOfWeek: Int): String {
        return when(dayOfWeek) {
            1 -> "monday"
            2 -> "tuesday"
            3 -> "wednesday"
            4 -> "thursday"
            5 -> "friday"
            6 -> "saturday"
            7 -> "sunday"
            else -> "monday"
        }
    }

    private suspend fun deleteAllEvents(timetableId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 먼저 모든 이벤트 목록 조회
                val response = timetableApi.getAllEvents(timetableId)
                if (!response.isSuccessful || response.body()?.isSuccess != true) {
                    Log.e(TAG, "이벤트 목록 조회 실패")
                    return@withContext false
                }

                val events = response.body()?.result ?: emptyList()
                
                // 각 이벤트 삭제
                var success = true
                events.forEach { event ->
                    try {
                        val deleteResponse = timetableApi.deleteEvent(timetableId, event.id)
                        if (!deleteResponse.isSuccessful) {
                            Log.e(TAG, "이벤트 삭제 실패: ${event.id}")
                            success = false
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "이벤트 삭제 중 오류", e)
                        success = false
                    }
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "모든 이벤트 삭제 중 오류 발생", e)
                false
            }
        }
    }

    private fun convertEventDtoToTimetableEntry(eventDto: EventDto): TimetableEntry {
        // "HH:mm" 형식의 시간 문자열을 시간과 분으로 파싱
        val startTimeParts = eventDto.startTime.split(":")
        val endTimeParts = eventDto.endTime.split(":")

        val dayOfWeek = when(eventDto.day.lowercase()) {
            "monday" -> 1
            "tuesday" -> 2
            "wednesday" -> 3
            "thursday" -> 4
            "friday" -> 5
            "saturday" -> 6
            "sunday" -> 7
            else -> 1
        }

        // color가 null인 경우 기본값 설정
        val color = eventDto.color ?: "#5B7BF5"  // 기본 파란색

        return TimetableEntry(
            id = eventDto.id,
            title = eventDto.title,
            dayOfWeek = dayOfWeek,
            startHour = startTimeParts[0].toIntOrNull() ?: 0,
            startMinute = startTimeParts.getOrNull(1)?.toIntOrNull() ?: 0,
            endHour = endTimeParts[0].toIntOrNull() ?: 0,
            endMinute = endTimeParts.getOrNull(1)?.toIntOrNull() ?: 0,
            location = "", // API에 location 필드가 없음
            colorHex = color
        )
    }
}