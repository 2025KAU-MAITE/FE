package com.example.maite.data.model

// UI에서 즉시 표시할 수 있는 알림 아이템
data class NotificationItem(
    val id: Int,
    val type: NotificationType,
    val senderName: String,
    val message: String,
    val profileImageRes: Int? = null,  // 프로필 이미지 리소스 (옵션)
    val profileImageUrl: String? = null,  // 프로필 이미지 URL (옵션)
    val timestamp: String? = null,
    val isAccepted: Boolean = false,  // 수락 여부
    val originalData: Any? = null  // 원본 데이터 (RoomNotification, MeetingNotification 등)
)
