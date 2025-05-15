package com.example.maite.data.model

// 회의방 초대 알림 데이터 모델
data class RoomNotification(
    val roomId: Int,
    val name: String,
    val hostEmail: String,
    val description: String
)

// 회의 제안 알림 데이터 모델
data class MeetingNotification(
    val meetingId: Long,
    val title: String,
    val proposerName: String,
    val meetingDate: String,
    val meetingTime: String,
    val address: String
)

// UI에서 사용할 통합 알림 모델
data class UnifiedNotification(
    val id: Int,
    val type: NotificationType,
    val title: String,
    val message: String,
    val userInfo: String,  // 보낸사람 이름이나 정보
    val timestamp: String? = null,
    val isAccepted: Boolean = false
)

// 알림 타입 enum
enum class NotificationType {
    FRIEND_REQUEST,    // 친구 요청
    MEETING_INVITE,    // 회의 제안
    ROOM_INVITE        // 회의방 초대
}
