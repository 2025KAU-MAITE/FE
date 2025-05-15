package com.example.maite.notification

import retrofit2.Response
import retrofit2.http.GET

interface NotificationApiService {
    
    // 회의방 초대 알림 조회
    @GET("/notifications/rooms")
    suspend fun getRoomInviteNotifications(): Response<List<RoomInviteNotification>>
    
    // 회의 제안 알림 조회
    @GET("/notifications/meetings")
    suspend fun getMeetingNotifications(): Response<List<MeetingNotification>>
}

// API 응답 모델
data class RoomInviteNotification(
    val roomId: Int?,
    val name: String?,  // 회의방 이름
    val hostEmail: String?,
    val hostName: String?,  // 호스트 이름 (추가 필드)
    val description: String?
)

data class MeetingNotification(
    val meetingId: Int?,
    val title: String?,
    val proposerName: String?,  // 제안자 이름
    val meetingDate: String?,
    val meetingTime: String?,
    val address: String?
)