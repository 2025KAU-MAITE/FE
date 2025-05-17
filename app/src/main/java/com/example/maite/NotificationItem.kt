package com.example.maite.model

import com.example.maite.notification.MeetingDetails

data class NotificationItem(
    val id: Int,
    val type: NotificationType,
    val senderName: String,
    val message: String,
    val profileImageRes: Int,
    val profileImageUrl: String? = null,  // 프로필 이미지 URL
    val senderEmail: String? = null,     // 보낸 사람의 이메일
    val roomId: Int? = null,          // 회의방 ID (ROOM_INVITE 타입일 때)
    val meetingId: Int? = null,       // 회의 ID (MEETING_INVITE 타입일 때)
    val meetingDetails: MeetingDetails? = null,  // 회의 상세정보
    val friendRequestId: Int? = null,  // 친구 요청 ID (FRIEND_REQUEST 타입일 때)
    val userId: Int? = null          // 요청한 사용자 ID (FRIEND_REQUEST 타입일 때)
)

enum class NotificationType {
    FRIEND_REQUEST,    // 친구 요청
    MEETING_INVITE,    // 회의 제안 
    ROOM_INVITE,       // 회의방 초대
    CHAT               // 채팅 알림
}
