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
    
    // 친구 요청 알림 조회
    @GET("/api/mates/requests")
    suspend fun getFriendRequestNotifications(): Response<ApiResponse<List<FriendRequestNotification>>>
}

// API 공통 응답 래퍼 구조
data class ApiResponse<T>(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: T
)

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

// 친구 요청 알림 모델
data class FriendRequestNotification(
    val requestId: Int?,
    val userId: Int?,
    val name: String?,
    val email: String?,
    val profileImageUrl: String?,
    val createdAt: String?
)