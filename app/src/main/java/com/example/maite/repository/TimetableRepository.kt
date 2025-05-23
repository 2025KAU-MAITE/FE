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
            Log.e(TAG, "대기 중 인터럽트 발생", e)
            Thread.currentThread().interrupt()
        }
    }

    // 사용자의 시간표 ID를 SharedPreferences에 저장
    private fun saveTimetableId(userId: Long, timetableId: Long) {
        Log.d(TAG, "시간표 ID 저장: userId=$userId, timetableId=$timetableId")
        preferencesUtil.saveLong("timetable_id_$userId", timetableId)
        // 마지막 업데이트 시간 저장 (동기화 충돌 방지 용)
        preferencesUtil.saveLong("timetable_updated_$userId", System.currentTimeMillis())
    }

    private fun getTimetableId(userId: Long): Long? {
        val timetableId = preferencesUtil.getLong("timetable_id_$userId")
        Log.d(TAG, "시간표 ID 조회: userId=$userId, timetableId=$timetableId")
        return timetableId
    }

    suspend fun createOrGetTimetable(userId: Long): Long? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "======= 시간표 ID 조회/생성 시작 =======")
                Log.d(TAG, "userId: $userId")
                
                // 먼저 getMyTimetable API를 통해 서버에서 직접 내 시간표 목록을 가져옴
                try {
                    Log.d(TAG, "getMyTimetable API 호출로 내 시간표 조회 시도")
                    val myTimetablesResponse = timetableApi.getMyTimetable()
                    
                    if (myTimetablesResponse.isSuccessful && myTimetablesResponse.body()?.isSuccess == true) {
                        val timetableResults = myTimetablesResponse.body()?.result
                        if (timetableResults != null && timetableResults.isNotEmpty()) {
                            // 첫 번째 시간표 ID 사용
                            val timetableId = timetableResults[0].id
                            Log.d(TAG, "✅ 서버에서 내 시간표 ID 직접 확인: $timetableId")
                            // 캐싱 및 저장
                            lastSuccessfulTimetableId = timetableId
                            saveTimetableId(userId, timetableId)
                            return@withContext timetableId
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "getMyTimetable API 호출 실패, 대체 방법으로 진행", e)
                }
                
                // 캐싱된 ID가 있으면 바로 사용 (많은 API 요청 방지)
                if (lastSuccessfulTimetableId != null) {
                    Log.d(TAG, "캐싱된 시간표 ID 사용: $lastSuccessfulTimetableId")
                    return@withContext lastSuccessfulTimetableId
                }
                
                // 기존에 저장된 timetableId가 있는지 확인
                val savedTimetableId = getTimetableId(userId)
                Log.d(TAG, "저장된 시간표 ID: $savedTimetableId")
                
                if (savedTimetableId != null) {
                    // 유효한지 확인 (GET 요청으로)
                    try {
                        Log.d(TAG, "기존 시간표 ID 유효성 확인 중...")
                        val response = timetableApi.getTimetable(savedTimetableId)
                        Log.d(TAG, "기존 시간표 검증 응답: isSuccessful=${response.isSuccessful}, code=${response.code()}")
                        
                        if (response.isSuccessful && response.body()?.isSuccess == true) {
                            Log.d(TAG, "✅ 기존 시간표 ID 유효함: $savedTimetableId")
                            // 시간표 ID 찾았을 때 캐싱 업데이트
                            lastSuccessfulTimetableId = savedTimetableId
                            return@withContext savedTimetableId
                        } else {
                            Log.w(TAG, "⚠️ 기존 시간표 ID가 유효하지 않음: $savedTimetableId, 새로 생성 필요")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ 기존 시간표 ID 확인 중 오류 발생", e)
                    }
                } else {
                    Log.d(TAG, "저장된 시간표 ID가 없음, 새로 생성 필요")
                }

                // 없거나 유효하지 않으면 새로 생성
                Log.d(TAG, "새 시간표 생성 시도 (userId: $userId)")
                val request = CreateTimetableRequest(title = "My Timetable", userId = userId)
                
                // 재시도 로직 추가 (최대 3회)
                var retryCount = 0
                val maxRetries = 3
                var lastException: Exception? = null
                
                while (retryCount < maxRetries) {
                    try {
                        val response = timetableApi.createTimetable(request)
                        Log.d(TAG, "시간표 생성 응답 [시도 ${retryCount+1}]: isSuccessful=${response.isSuccessful}, code=${response.code()}")

                        if (response.isSuccessful && response.body()?.isSuccess == true) {
                            val timetableId = response.body()?.result?.id
                            Log.d(TAG, "✅ 새 시간표 생성 성공: timetableId=$timetableId")
                            
                            if (timetableId != null) {
                                saveTimetableId(userId, timetableId)
                                // 시간표 ID 캐싱 업데이트
                                lastSuccessfulTimetableId = timetableId
                                return@withContext timetableId
                            }
                            
                            break  // ID가 null이면 더 이상 시도하지 않음
                        } else {
                            Log.e(TAG, "❌ 시간표 생성 실패 [시도 ${retryCount+1}]: ${response.errorBody()?.string()}")
                            retryCount++
                            if (retryCount < maxRetries) {
                                // Thread.sleep을 사용하여 안전하게 딜레이 수행
                                safeDelay((500 * (retryCount + 1)).toLong())
                            }
                        }
                    } catch (e: Exception) {
                        lastException = e
                        Log.e(TAG, "❌ 시간표 생성 중 예외 발생 [시도 ${retryCount+1}]", e)
                        retryCount++
                        if (retryCount < maxRetries) {
                            // Thread.sleep을 사용하여 안전하게 딜레이 수행
                            safeDelay((500 * (retryCount + 1)).toLong())
                        }
                    }
                }
                
                // 모든 시도 실패 시
                if (lastException != null) {
                    Log.e(TAG, "❌ 모든 시도 후 시간표 생성 실패", lastException)
                } else {
                    Log.e(TAG, "❌ 시간표 생성 실패: 원인 불명")
                }
                
                Log.d(TAG, "======= 시간표 ID 조회/생성 종료 =======")
                null  // 실패 시 null 반환
            } catch (e: Exception) {
                Log.e(TAG, "❌ 시간표 생성/조회 중 최상위 오류 발생", e)
                null
            }
        }
    }

    suspend fun saveTimetable(userId: Long, entries: List<TimetableEntry>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "===== 시간표 저장 시작 =====")
                Log.d(TAG, "userId=$userId, 항목 수=${entries.size}")
                Log.d(TAG, "저장할 일정 리스트 로깅:")
                entries.forEachIndexed { index, entry ->
                    Log.d(TAG, "[$index] 제목: ${entry.title}, 요일: ${entry.dayOfWeek}, 시간: ${entry.startHour}:${entry.startMinute}-${entry.endHour}:${entry.endMinute}")
                }
                
                // 1. 내 시간표 조회 API 호출 - 기존 시간표가 있는지 확인
                var existingTimetableId: Long? = null
                var retryCount = 0
                val maxRetries = 3
                
                while (retryCount < maxRetries && existingTimetableId == null) {
                    try {
                        Log.d(TAG, "내 시간표 조회 API 호출 [시도 ${retryCount+1}]")
                        val myTimetablesResponse = timetableApi.getMyTimetable()
                        
                        if (myTimetablesResponse.isSuccessful && myTimetablesResponse.body()?.isSuccess == true) {
                            val timetableResults = myTimetablesResponse.body()?.result
                            if (timetableResults != null && timetableResults.isNotEmpty()) {
                                // 첫 번째 시간표 ID 사용
                                existingTimetableId = timetableResults[0].id
                                Log.d(TAG, "✅ 기존 시간표 ID 확인 성공: $existingTimetableId")
                                // 캐싱 업데이트 및 저장
                                lastSuccessfulTimetableId = existingTimetableId
                                saveTimetableId(userId, existingTimetableId)
                                break
                            } else {
                                Log.d(TAG, "시간표 조회 결과 없음, 새 시간표 생성 필요")
                            }
                        } else {
                            Log.w(TAG, "시간표 조회 실패 - 상태코드: ${myTimetablesResponse.code()}")
                        }
                        
                        retryCount++
                        if (retryCount < maxRetries && existingTimetableId == null) {
                            safeDelay(200L * retryCount)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "내 시간표 조회 중 오류 발생 [시도 ${retryCount+1}]", e)
                        retryCount++
                        if (retryCount < maxRetries) {
                            safeDelay(200L * retryCount)
                        }
                    }
                }
                
                // 2. 내 시간표 조회로 ID를 찾지 못했다면 기존 방식으로 ID 조회/생성
                val timetableId = existingTimetableId ?: createOrGetTimetable(userId)
                if (timetableId == null) {
                    Log.e(TAG, "❌ 시간표 ID를 가져올 수 없습니다")
                    return@withContext false
                }
                
                Log.d(TAG, "저장할 시간표 ID: $timetableId (새로 생성 여부: ${existingTimetableId == null})")

                // 기존 이벤트 조회 후 비교 - 필요한 경우에만 삭제 후 생성
                Log.d(TAG, "현재 서버의 이벤트와 로컬 이벤트 비교 분석...")

                val serverEvents = try {
                    val eventsResponse = timetableApi.getAllEvents(timetableId)
                    if (eventsResponse.isSuccessful && eventsResponse.body()?.isSuccess == true) {
                        eventsResponse.body()?.result ?: emptyList()
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "서버 이벤트 조회 실패", e)
                    emptyList()
                }

                // 시간표가 완전히 달라졌는지 확인
                val serverEntryCount = serverEvents.size
                val localEntryCount = entries.size
                val significantChange = Math.abs(serverEntryCount - localEntryCount) > 1 || localEntryCount == 0

                // 기존 이벤트 삭제
                Log.d(TAG, "기존 이벤트 삭제 요청...")
                val deleteSuccess = deleteAllEvents(timetableId)
                if (!deleteSuccess) {
                    Log.e(TAG, "❌ 기존 이벤트 삭제 실패")
                    // 삭제 실패해도 계속 진행하도록 변경 (일부 이벤트만 삭제에 실패한 경우 허용)
                    Log.w(TAG, "삭제 실패에도 불구하고 이벤트 추가를 시도합니다")
                } else {
                    Log.d(TAG, "✅ 기존 이벤트 삭제 성공!")
                }
                
                // 저장할 이벤트가 없으면 성공으로 간주
                if (entries.isEmpty()) {
                    Log.d(TAG, "저장할 이벤트가 없음, 성공으로 처리합니다.")
                    return@withContext true
                }
                
                // 성공/실패 카운터 추가
                var successCount = 0
                var failCount = 0
                
                // 각 이벤트를 서버에 저장 (색상 배정 적용)
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
                    
                    Log.d(TAG, "이벤트 저장 요청 [${index+1}/${entries.size}]: " +
                            "title=${entryWithColor.title}, " +
                            "day=${getDayString(entryWithColor.dayOfWeek)}, " +
                            "time=${entryWithColor.startHour}:${entryWithColor.startMinute}-${entryWithColor.endHour}:${entryWithColor.endMinute}, " +
                            "color=${entryWithColor.colorHex}, " +
                            "place=${entryWithColor.location ?: "없음"}")
                    
                    try {
                        // 최대 3번 재시도
                        var retryCount = 0
                        var success = false
                        var lastError: Exception? = null
                        
                        while (retryCount < 3 && !success) {
                            try {
                                val response = timetableApi.createEvent(timetableId, eventRequest)
                                
                                if (response.isSuccessful && response.body()?.isSuccess == true) {
                                    Log.d(TAG, "✅ 이벤트 저장 성공 [${index+1}/${entries.size}]: ${entry.title}")
                                    success = true
                                    successCount++
                                    break
                                } else {
                                    val errorCode = response.code()
                                    val errorMsg = response.errorBody()?.string() ?: "알 수 없는 오류"
                                    Log.e(TAG, "❌ 이벤트 저장 실패 [시도 ${retryCount+1}] [${index+1}/${entries.size}]: ${entry.title}, 응답 코드: $errorCode, 오류: $errorMsg")
                                    retryCount++
                                    
                                    if (retryCount < 3) {
                                        // 재시도 전 대기
                                        safeDelay(300L * (retryCount))
                                    }
                                }
                            } catch (e: Exception) {
                                lastError = e
                                Log.e(TAG, "❌ 이벤트 저장 중 예외 발생 [시도 ${retryCount+1}] [${index+1}/${entries.size}]: ${entry.title}", e)
                                retryCount++
                                
                                if (retryCount < 3) {
                                    // 재시도 전 대기
                                    safeDelay(300L * (retryCount))
                                }
                            }
                        }
                        
                        if (!success) {
                            failCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ 이벤트 저장 중 최상위 예외 발생 [${index+1}/${entries.size}]: ${entry.title}", e)
                        failCount++
                    }
                }

                // 모든 항목이 실패한 경우만 실패로 처리 (일부 성공은 성공으로 간주)
                val savingSuccess = successCount > 0
                Log.d(TAG, "===== 시간표 저장 완료 =====")
                Log.d(TAG, "성공: ${successCount}개, 실패: ${failCount}개, 최종 결과: ${if(savingSuccess) "성공 ✅" else "실패 ❌"}")

                // 저장 성공 시 시간표 ID 저장 (실패해도 시도는 했으므로 저장)
                if (savingSuccess) {
                    saveTimetableId(userId, timetableId)
                }
                
                savingSuccess
            } catch (e: Exception) {
                Log.e(TAG, "❌ 시간표 저장 중 오류 발생", e)
                false
            }
        }
    }

    suspend fun loadTimetable(userId: Long): List<TimetableEntry> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "===== 시간표 로드 시작 =====")
                Log.d(TAG, "userId=$userId")
                
                // 최대 3회 재시도
                var retryCount = 0
                val maxRetries = 3
                var lastException: Exception? = null
                
                // 새로운 로직: 먼저 /api/timetables/my 엔드포인트로 시도
                while (retryCount < maxRetries) {
                    try {
                        // 새 엔드포인트로 내 시간표 직접 요청
                        Log.d(TAG, "getMyTimetable API 호출 [시도 ${retryCount+1}]")
                        val response = timetableApi.getMyTimetable()
                        Log.d(TAG, "API 응답 [시도 ${retryCount+1}]: isSuccessful=${response.isSuccessful}, code=${response.code()}")
                        
                        if (response.isSuccessful && response.body()?.isSuccess == true) {
                            val timetableResults = response.body()?.result
                            
                            if (timetableResults != null && timetableResults.isNotEmpty()) {
                                // 첫 번째 시간표를 사용 (우선 순위가 높은 것으로 간주)
                                val timetable = timetableResults[0]
                                Log.d(TAG, "✅ 내 시간표 조회 성공: ID=${timetable.id}")
                                
                                // 시간표 ID 저장 (나중에 사용하기 위해)
                                saveTimetableId(userId, timetable.id)
                                
                                val events = timetable.events
                                Log.d(TAG, "✅ 서버에서 ${events.size}개 이벤트 불러옴")
                                
                                val timetableEntries = events.map { eventDto ->
                                    Log.d(TAG, "이벤트 변환: ${eventDto.title}, ${eventDto.day}, ${eventDto.startTime}-${eventDto.endTime}, 장소=${eventDto.place}")
                                    convertEventDtoToTimetableEntry(eventDto)
                                }
                                
                                Log.d(TAG, "===== 시간표 로드 완료 =====")
                                Log.d(TAG, "변환 완료: ${timetableEntries.size}개 시간표 항목")
                                
                                return@withContext timetableEntries
                            } else {
                                Log.d(TAG, "내 시간표가 비어있음, 새로운 시간표 생성 필요")
                                break  // 기존 로직으로 전환
                            }
                        } else if (response.code() == 404) {
                            // 내 시간표가 없는 경우 (첫 사용자)
                            Log.d(TAG, "내 시간표가 없음, 새로운 시간표 생성 필요")
                            break  // 기존 로직으로 전환
                        } else {
                            Log.e(TAG, "❌ 내 시간표 조회 실패 [시도 ${retryCount+1}]: ${response.errorBody()?.string()}, 응답 코드: ${response.code()}")
                            
                            retryCount++
                            if (retryCount < maxRetries) {
                                val waitTime = 500L * (retryCount + 1)
                                Log.d(TAG, "⏱️ 다음 시도 전 ${waitTime}ms 대기")
                                safeDelay(waitTime)
                            }
                        }
                    } catch (e: Exception) {
                        lastException = e
                        Log.e(TAG, "❌ 내 시간표 조회 중 예외 발생 [시도 ${retryCount+1}]", e)
                        retryCount++
                        
                        if (retryCount < maxRetries) {
                            val waitTime = 500L * (retryCount + 1)
                            Log.d(TAG, "⏱️ 다음 시도 전 ${waitTime}ms 대기")
                            safeDelay(waitTime)
                        }
                    }
                }
                
                // 내 시간표 조회 실패 시 기존 로직으로 시도 (시간표 ID 기반)
                Log.d(TAG, "내 시간표 API 실패, 기존 방식으로 시도")
                
                // 시간표 ID 가져오기 (없으면 생성)
                val timetableId = createOrGetTimetable(userId)
                if (timetableId == null) {
                    Log.e(TAG, "❌ 시간표 ID를 가져올 수 없습니다: userId=$userId")
                    return@withContext emptyList()
                }
                
                Log.d(TAG, "✅ 시간표 ID 획득: timetableId=$timetableId")
                
                // 기존 ApiClient 인스턴스를 재활용하여 API 호출
                // 캐시 문제를 방지하기 위해 resetClient를 사용
                val refreshedApi = ApiClient.resetClient(context).create(TimetableApi::class.java)
                
                // 최대 3회 재시도 (기존 방식)
                retryCount = 0
                lastException = null
                
                while (retryCount < maxRetries) {
                    try {
                        // 이벤트 가져오기
                        Log.d(TAG, "getAllEvents API 호출 [시도 ${retryCount+1}]: timetableId=$timetableId")
                        val response = refreshedApi.getAllEvents(timetableId)
                        Log.d(TAG, "API 응답 [시도 ${retryCount+1}]: isSuccessful=${response.isSuccessful}, code=${response.code()}")
                        
                        if (response.isSuccessful && response.body()?.isSuccess == true) {
                            val events = response.body()?.result ?: emptyList()
                            Log.d(TAG, "✅ 서버에서 ${events.size}개 이벤트 불러옴")
                            
                            // 응답 본문 로깅 추가
                            val responseBody = response.body()
                            Log.d(TAG, "API 응답 메시지: ${responseBody?.message}")
                            Log.d(TAG, "API 응답 코드: ${responseBody?.code}")
                            
                            val timetableEntries = events.map { eventDto ->
                                Log.d(TAG, "이벤트 변환: ${eventDto.title}, ${eventDto.day}, ${eventDto.startTime}-${eventDto.endTime}, 장소=${eventDto.place}")
                                convertEventDtoToTimetableEntry(eventDto)
                            }
                            
                            Log.d(TAG, "===== 시간표 로드 완료 =====")
                            Log.d(TAG, "변환 완료: ${timetableEntries.size}개 시간표 항목")
                            
                            // 성공한 경우 결과 반환
                            return@withContext timetableEntries
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
                            Log.e(TAG, "❌ 이벤트 로드 실패 [시도 ${retryCount+1}]: $errorBody, 응답 코드: ${response.code()}")
                            
                            // 401, 403 같은 권한 문제인 경우 시간표 ID를 초기화하고 다시 시도
                            if (response.code() == 401 || response.code() == 403) {
                                Log.w(TAG, "⚠️ 권한 문제 감지, 시간표 ID 초기화 후 재시도")
                                // remove 대신 키 값을 null로 저장하여 삭제 효과
                                preferencesUtil.saveLong("timetable_id_$userId", 0)
                                
                                // 시간표 ID 재생성 시도
                                val newTimetableId = createOrGetTimetable(userId)
                                if (newTimetableId != null) {
                                    Log.d(TAG, "✅ 새 시간표 ID 획득: $newTimetableId")
                                }
                            }
                            
                            retryCount++
                            if (retryCount < maxRetries) {
                                val waitTime = 500L * (retryCount + 1)
                                Log.d(TAG, "⏱️ 다음 시도 전 ${waitTime}ms 대기")
                                safeDelay(waitTime)
                            }
                        }
                    } catch (e: Exception) {
                        lastException = e
                        Log.e(TAG, "❌ 이벤트 로드 중 예외 발생 [시도 ${retryCount+1}]", e)
                        retryCount++
                        
                        if (retryCount < maxRetries) {
                            val waitTime = 500L * (retryCount + 1)
                            Log.d(TAG, "⏱️ 다음 시도 전 ${waitTime}ms 대기")
                            safeDelay(waitTime)
                        }
                    }
                }
                
                // 모든 시도 실패 시
                if (lastException != null) {
                    Log.e(TAG, "❌ 모든 시도 후 시간표 로드 실패", lastException)
                } else {
                    Log.e(TAG, "❌ 시간표 로드 실패: 원인 불명")
                }
                
                Log.d(TAG, "===== 시간표 로드 실패 =====")
                emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "❌ 시간표 로드 중 최상위 오류 발생", e)
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
                var failCount = 0
                
                events.forEach { event ->
                    try {
                        val deleteResponse = timetableApi.deleteEvent(timetableId, event.id)
                        if (!deleteResponse.isSuccessful) {
                            Log.e(TAG, "이벤트 삭제 실패: ${event.id}")
                            failCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "이벤트 삭제 중 오류", e)
                        failCount++
                    }
                }
                
                // 최대 1개까지 실패 허용 (완전한 실패가 아니라면 계속 진행)
                if (failCount > events.size / 2) {
                    Log.e(TAG, "이벤트 삭제 중 너무 많은 실패 발생: $failCount / ${events.size}")
                    success = false
                } else {
                    Log.d(TAG, "이벤트 삭제 완료: 성공=${events.size - failCount}개, 실패=${failCount}개")
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
            location = eventDto.place ?: "", // place를 location으로 변환
            colorHex = color
        )
    }
}