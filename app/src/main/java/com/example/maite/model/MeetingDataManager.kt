package com.example.maite.model

import android.content.Context
import android.util.Log
import com.example.maite.MaiteApiService
import com.example.maite.MaiteRetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeetingDataManager(private val context: Context) {
    private val apiService: MaiteApiService = MaiteRetrofitClient.getInstance(context)
    private val meetListRepository = MeetListRepository.getInstance()
    private val propMeetRepository = PropMeetRepository.getInstance()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun fetchAndDistributeMeetings(roomId: Long): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val response = apiService.getMeetingsByRoom(roomId)

                if (response.isSuccessful && response.body() != null) {
                    // API 응답에서 회의 목록 가져오기
                    val meetings = response.body()!!
                    val currentDate = Date() // 현재 날짜

                    // 기존 데이터 초기화
                    meetListRepository.clearMeetings()
                    propMeetRepository.clearProposals()

                    // 과거 회의 (meetingDate < currentDate)
                    val pastMeetings = mutableListOf<MeetListItem>()

                    // 미래 회의 (meetingDate >= currentDate)
                    val futureMeetings = mutableListOf<PropMeetItem>()

                    // 디버그 로그 추가
                    Log.d("MeetingDataManager", "가져온 회의 수: ${meetings.size}")

                    // 각 회의를 적절한 리스트에 분류
                    for (meeting in meetings) {
                        try {
                            // 디버그 로그를 통해 날짜 확인
                            Log.d("MeetingDataManager", "처리 중인 회의 날짜: ${meeting.meetingDate}")

                            val meetingDate = dateFormat.parse(meeting.meetingDate)

                            // 필드 존재 여부 확인
                            val id = meeting.meetingId
                            val title = meeting.title.orEmpty()
                            val date = meeting.meetingDate.orEmpty()
                            val time = meeting.meetingTime.orEmpty()
                            val endTime = meeting.meetingEndTime.orEmpty() // 새 필드 추가
                            // place 필드가 null인 경우 기본값 설정
                            val place = meeting.address ?: "장소가 정해지지 않았습니다."

                            if (meetingDate != null) {
                                if (meetingDate.before(currentDate)) {
                                    // 과거 회의
                                    Log.d("MeetingDataManager", "과거 회의로 분류: ${meeting.title}")
                                    pastMeetings.add(MeetListItem(
                                        meetingId = id,
                                        title = title,
                                        date = date,
                                        time = time,
                                        endTime = endTime, // 새 필드 추가
                                        place = place
                                    ))
                                } else {
                                    // 미래 회의 (제안)
                                    Log.d("MeetingDataManager", "미래 회의로 분류: ${meeting.title}")
                                    futureMeetings.add(PropMeetItem(
                                        meetingId = id,
                                        title = title,
                                        date = date,
                                        time = time,
                                        endTime = endTime, // 새 필드 추가
                                        place = place,
                                        acceptance = meeting.acceptance ?: "PENDING"
                                    ))
                                }
                            } else {
                                Log.w("MeetingDataManager", "날짜 파싱 후 null 값: ${meeting.meetingDate}")
                            }
                        } catch (e: Exception) {
                            Log.e("MeetingDataManager", "날짜 파싱 오류: ${meeting.meetingDate} (Ask Gemini)", e)
                        }
                    }

                    // 분류된 회의를 적절한 저장소에 추가
                    meetListRepository.addAllMeetings(pastMeetings)
                    propMeetRepository.addAllProposals(futureMeetings)

                    Log.d("MeetingDataManager",
                        "회의 분류 완료: 과거 ${pastMeetings.size}개, 미래 ${futureMeetings.size}개")

                    true
                } else {
                    Log.e("MeetingDataManager",
                        "API 호출 실패: ${response.code()}, ${response.errorBody()?.string()}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("MeetingDataManager", "회의 데이터 가져오기 중 예외 발생", e)
            false
        }
    }

    fun getMeetListRepository(): MeetListRepository {
        return meetListRepository
    }

    fun getPropMeetRepository(): PropMeetRepository {
        return propMeetRepository
    }
}