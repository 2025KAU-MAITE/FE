package com.example.maite.repository

import android.content.Context
import com.example.maite.ApiClient
import com.example.maite.api.*
import com.example.maite.model.TimetableEntry
import com.example.maite.PreferencesUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.util.concurrent.TimeUnit
import com.example.maite.util.TimetableColorManager

class TimetableRepository(private val context: Context) {

    private val TAG = "TimetableRepository"
    private val timetableApi = ApiClient.getClient(context).create(TimetableApi::class.java)
    private val preferencesUtil = PreferencesUtil(context)
    private val colorManager = TimetableColorManager(context)
    
    // 마지막으로 성공적으로 가져온 시간표 ID를 캐싱
    private var lastSuccessfulTimetableId: Long? = null
    
    // Thread.sleep을 사용하여 딜레이를 안전하게 구현하는 도우미 함수
    private fun safeDelay(milliseconds: Long) {
        try {
            Thread.sleep(milliseconds)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    // 사용자의 시간표 ID를 SharedPreferences에 저장
    private fun saveTimetableId(userId: Long, timetableId: Long) {
        preferencesUtil.saveLong("timetable_id_$userId", timetableId)
        // 마지막 업데이트 시간 저장 (동기화 충돌 방지 용)
        preferencesUtil.saveLong("timetable_updated_$userId", System.currentTimeMillis())
    }

    private fun getTimetableId(userId: Long): Long? {
        val timetableId = preferencesUtil.getLong("timetable_id_$userId")
        return timetableId
    }

    suspend fun createOrGetTimetable(userId: Long): Long? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 먼저 getMyTimetable API를 통해 기존 시간표가 있는지 확인
                try {
                    val myTimetablesResponse = timetableApi.getMyTimetable()
                    
                    if (myTimetablesResponse.isSuccessful && myTimetablesResponse.body()?.isSuccess == true) {
                        val timetableResult = myTimetablesResponse.body()?.result
                        if (timetableResult != null) {
                            // 기존 시간표가 있으면 해당 ID 반환
                            val timetableId = timetableResult.id
                            // 캐싱 및 저장
                            lastSuccessfulTimetableId = timetableId
                            saveTimetableId(userId, timetableId)
                            Log.d(TAG, "기존 시간표 ID 발견: $timetableId")
                            return@withContext timetableId
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "getMyTimetable API 호출 실패, 다른 방법 시도", e)
                }
                
                // 2. 로컬에 저장된 ID가 있으면 검증 후 사용
                val savedTimetableId = getTimetableId(userId)
                if (savedTimetableId != null && savedTimetableId > 0) {
                    try {
                        val response = timetableApi.getTimetable(savedTimetableId)
                        if (response.isSuccessful && response.body()?.isSuccess == true) {
                            lastSuccessfulTimetableId = savedTimetableId
                            Log.d(TAG, "저장된 시간표 ID 유효성 확인: $savedTimetableId")
                            return@withContext savedTimetableId
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "저장된 시간표 ID 검증 실패: $savedTimetableId", e)
                    }
                }

                // 3. 새 시간표 생성 (중복 방지 로직 추가)
                Log.d(TAG, "새 시간표 생성 시도")
                val request = CreateTimetableRequest(title = "My Timetable", userId = userId)
                
                try {
                    val response = timetableApi.createTimetable(request)
                    
                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        val timetableId = response.body()?.result?.id
                        if (timetableId != null) {
                            saveTimetableId(userId, timetableId)
                            lastSuccessfulTimetableId = timetableId
                            Log.d(TAG, "새 시간표 생성 성공: $timetableId")
                            return@withContext timetableId
                        }
                    } else {
                        // 시간표 생성 실패 시 다시 한번 기존 시간표 조회 시도
                        Log.w(TAG, "시간표 생성 실패, 기존 시간표 재조회")
                        val retryResponse = timetableApi.getMyTimetable()
                        if (retryResponse.isSuccessful && retryResponse.body()?.isSuccess == true) {
                            val timetableResult = retryResponse.body()?.result
                            if (timetableResult != null) {
                                val timetableId = timetableResult.id
                                lastSuccessfulTimetableId = timetableId
                                saveTimetableId(userId, timetableId)
                                Log.d(TAG, "재조회에서 기존 시간표 발견: $timetableId")
                                return@withContext timetableId
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "시간표 생성 중 예외 발생", e)
                }
                
                Log.e(TAG, "시간표 ID 획득 실패")
                null
            } catch (e: Exception) {
                Log.e(TAG, "createOrGetTimetable 전체 실패", e)
                null
            }
        }
    }

    suspend fun saveTimetable(userId: Long, entries: List<TimetableEntry>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "시간표 저장 시작: userId=$userId, 항목 수=${entries.size}")
                
                // 1. 기존 시간표 ID 조회/생성
                val timetableId = createOrGetTimetable(userId)
                if (timetableId == null) {
                    Log.e(TAG, "시간표 ID 획득 실패")
                    return@withContext false
                }
                
                Log.d(TAG, "사용할 시간표 ID: $timetableId")

                // 2. 기존 이벤트 모두 삭제
                val deleteSuccess = deleteAllEvents(timetableId)
                if (!deleteSuccess) {
                    Log.w(TAG, "기존 이벤트 삭제 일부 실패, 계속 진행")
                }
                
                // 3. 저장할 이벤트가 없으면 성공으로 간주
                if (entries.isEmpty()) {
                    Log.d(TAG, "저장할 이벤트가 없음, 삭제만 완료")
                    return@withContext true
                }
                
                // 4. 새 이벤트들 생성
                var successCount = 0
                var failCount = 0
                
                entries.forEachIndexed { index, entry ->
                    // 색상이 설정되어 있지 않은 경우 색상 배정
                    val entryWithColor = if (entry.colorHex.isEmpty() || entry.colorHex == "#4C7EED") {
                        colorManager.assignColor(entry)
                    } else {
                        entry
                    }
                    
                    val eventRequest = CreateEventRequest(
                        title = entryWithColor.title,
                        day = getDayString(entryWithColor.dayOfWeek),
                        color = entryWithColor.colorHex,
                        startTime = String.format("%02d:%02d", entryWithColor.startHour, entryWithColor.startMinute),
                        endTime = String.format("%02d:%02d", entryWithColor.endHour, entryWithColor.endMinute),
                        place = entryWithColor.location
                    )
                    
                    try {
                        val response = timetableApi.createEvent(eventRequest)
                        
                        if (response.isSuccessful && response.body()?.isSuccess == true) {
                            successCount++
                            Log.d(TAG, "이벤트 생성 성공: ${entryWithColor.title}")
                        } else {
                            failCount++
                            Log.w(TAG, "이벤트 생성 실패: ${entryWithColor.title}, 응답: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        failCount++
                        Log.e(TAG, "이벤트 생성 중 예외: ${entryWithColor.title}", e)
                    }
                }

                val savingSuccess = successCount > 0 || entries.isEmpty()
                Log.d(TAG, "시간표 저장 완료: 성공=$successCount, 실패=$failCount, 전체성공=$savingSuccess")
                
                // 저장 성공 시 시간표 ID 저장
                if (savingSuccess) {
                    saveTimetableId(userId, timetableId)
                }
                
                return@withContext savingSuccess
            } catch (e: Exception) {
                Log.e(TAG, "시간표 저장 중 예외 발생", e)
                return@withContext false
            }
        }
    }

    suspend fun loadTimetable(userId: Long): List<TimetableEntry> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "시간표 로드 시작: userId=$userId")
                
                // 1. getMyTimetable API로 직접 조회 시도 (가장 확실한 방법)
                try {
                    val response = timetableApi.getMyTimetable()
                    
                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        val timetableResult = response.body()?.result
                        
                        if (timetableResult != null) {
                            // 시간표 ID 저장
                            saveTimetableId(userId, timetableResult.id)
                            lastSuccessfulTimetableId = timetableResult.id
                            
                            val events = timetableResult.events
                            val timetableEntries = events.map { eventDto ->
                                convertEventDtoToTimetableEntry(eventDto)
                            }
                            
                            Log.d(TAG, "getMyTimetable로 시간표 로드 성공: ${timetableEntries.size}개 항목")
                            return@withContext timetableEntries
                        } else {
                            Log.d(TAG, "getMyTimetable 응답은 성공이지만 시간표가 없음")
                        }
                    } else if (response.code() == 404) {
                        Log.d(TAG, "시간표가 아직 생성되지 않음 (404)")
                        return@withContext emptyList()
                    } else {
                        Log.w(TAG, "getMyTimetable 실패: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "getMyTimetable API 호출 실패", e)
                }
                
                // 2. 기존 방식으로 시간표 ID 조회 후 이벤트 로드
                val timetableId = createOrGetTimetable(userId)
                if (timetableId == null) {
                    Log.w(TAG, "시간표 ID 획득 실패")
                    return@withContext emptyList()
                }
                
                Log.d(TAG, "시간표 ID로 이벤트 조회: $timetableId")
                
                try {
                    val response = timetableApi.getAllEvents()
                    
                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        val events = response.body()?.result ?: emptyList()
                        
                        val timetableEntries = events.map { eventDto ->
                            convertEventDtoToTimetableEntry(eventDto)
                        }
                        
                        Log.d(TAG, "이벤트 조회로 시간표 로드 성공: ${timetableEntries.size}개 항목")
                        return@withContext timetableEntries
                    } else {
                        Log.w(TAG, "이벤트 조회 실패: ${response.code()}")
                        
                        // 권한 문제인 경우 시간표 ID 초기화
                        if (response.code() == 401 || response.code() == 403) {
                            preferencesUtil.saveLong("timetable_id_$userId", 0)
                            lastSuccessfulTimetableId = null
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "이벤트 조회 중 예외 발생", e)
                }
                
                Log.w(TAG, "모든 시간표 로드 방법 실패, 빈 리스트 반환")
                return@withContext emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "시간표 로드 중 전체 예외 발생", e)
                return@withContext emptyList()
            }
        }
    }

    // 실시간 이벤트 추가
    suspend fun addEventToServer(userId: Long, entry: TimetableEntry): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "실시간 이벤트 추가 시작: ${entry.title}")
                
                // 시간표 ID 조회/생성
                val timetableId = createOrGetTimetable(userId)
                if (timetableId == null) {
                    Log.e(TAG, "시간표 ID 획득 실패")
                    return@withContext false
                }
                
                // 색상 배정
                val entryWithColor = if (entry.colorHex.isEmpty() || entry.colorHex == "#4C7EED") {
                    colorManager.assignColor(entry)
                } else {
                    entry
                }
                
                // 이벤트 생성 요청
                val eventRequest = CreateEventRequest(
                    title = entryWithColor.title,
                    day = getDayString(entryWithColor.dayOfWeek),
                    color = entryWithColor.colorHex,
                    startTime = String.format("%02d:%02d", entryWithColor.startHour, entryWithColor.startMinute),
                    endTime = String.format("%02d:%02d", entryWithColor.endHour, entryWithColor.endMinute),
                    place = entryWithColor.location
                )
                
                val response = timetableApi.createEvent(eventRequest)
                
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    Log.d(TAG, "실시간 이벤트 추가 성공: ${entryWithColor.title}")
                    return@withContext true
                } else {
                    Log.w(TAG, "실시간 이벤트 추가 실패: ${response.code()}")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "실시간 이벤트 추가 중 예외 발생", e)
                return@withContext false
            }
        }
    }
    
    // 실시간 이벤트 삭제 (eventId 기반)
    suspend fun deleteEventFromServer(userId: Long, eventId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "실시간 이벤트 삭제 시작: eventId=$eventId")
                
                // 시간표 ID 조회
                val timetableId = createOrGetTimetable(userId)
                if (timetableId == null) {
                    Log.e(TAG, "시간표 ID 획득 실패")
                    return@withContext false
                }
                
                val response = timetableApi.deleteEvent(eventId)
                
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    Log.d(TAG, "실시간 이벤트 삭제 성공: eventId=$eventId")
                    return@withContext true
                } else {
                    Log.w(TAG, "실시간 이벤트 삭제 실패: ${response.code()}")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "실시간 이벤트 삭제 중 예외 발생", e)
                return@withContext false
            }
        }
    }
    
    // 실시간 전체 이벤트 삭제 (초기화)
    suspend fun clearAllEventsFromServer(userId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "실시간 전체 이벤트 삭제 시작")
                
                // 시간표 ID 조회
                val timetableId = createOrGetTimetable(userId)
                if (timetableId == null) {
                    Log.e(TAG, "시간표 ID 획득 실패")
                    return@withContext false
                }
                
                return@withContext deleteAllEvents(timetableId)
            } catch (e: Exception) {
                Log.e(TAG, "실시간 전체 이벤트 삭제 중 예외 발생", e)
                return@withContext false
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
                val response = timetableApi.getAllEvents()
                if (!response.isSuccessful || response.body()?.isSuccess != true) {
                    return@withContext false
                }

                val events = response.body()?.result ?: emptyList()
                
                // 각 이벤트 삭제
                var success = true
                var failCount = 0
                
                events.forEach { event ->
                    try {
                        val deleteResponse = timetableApi.deleteEvent(event.id)
                        if (!deleteResponse.isSuccessful) {
                            failCount++
                        }
                    } catch (e: Exception) {
                        failCount++
                    }
                }
                
                // 최대 1개까지 실패 허용 (완전한 실패가 아니라면 계속 진행)
                if (failCount > events.size / 2) {
                    success = false
                }
                
                success
            } catch (e: Exception) {
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
            location = eventDto.place ?: "", // place를 location으로 변환
            colorHex = color
        )
    }
}
