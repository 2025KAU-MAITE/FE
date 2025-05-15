package com.example.maite.notification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maite.data.model.MeetingProposal
import com.example.maite.data.model.ProposalType
import com.example.maite.data.repository.ProposalRepository
import com.example.maite.model.NotificationItem
import com.example.maite.model.NotificationType
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val repository: NotificationRepository,
    private val proposalRepository: ProposalRepository
) : ViewModel() {
    
    private val _notifications = MutableLiveData<List<NotificationItem>>()
    val notifications: LiveData<List<NotificationItem>> = _notifications
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val allNotifications = mutableListOf<NotificationItem>()
                var notificationId = 1
                
                // 회의방 초대 알림 조회
                val roomInvitesResult = repository.getRoomInviteNotifications()
                roomInvitesResult.onSuccess { roomInvites ->
                    roomInvites.forEach { invite ->
                        if (invite.roomId != null && invite.name != null) {
                            // 호스트 이름 결정
                            // 1. API에 hostName이 있으면 사용
                            // 2. 없으면 email에서 @ 앞부분 사용
                            val hostName = invite.hostName ?: invite.hostEmail?.substringBefore("@") ?: "알 수 없음"
                            
                            allNotifications.add(
                                NotificationItem(
                                    id = invite.roomId,  
                                    type = NotificationType.ROOM_INVITE,
                                    senderName = invite.name,  // 회의방 이름
                                    message = "${hostName}의 회의방 초대를 받았어요.",
                                    profileImageRes = com.example.maite.R.drawable.ic_launcher_foreground,
                                    roomId = invite.roomId
                                )
                            )
                        }
                    }
                }
                
                // 회의 제안 알림 조회
                val meetingNotificationsResult = repository.getMeetingNotifications()
                meetingNotificationsResult.onSuccess { meetingNotifications ->
                    meetingNotifications.forEach { notification ->
                        if (notification.meetingId != null && notification.title != null && notification.proposerName != null) {
                            allNotifications.add(
                                NotificationItem(
                                    id = notification.meetingId,  
                                    type = NotificationType.MEETING_INVITE,
                                    senderName = notification.proposerName,  
                                    message = "${notification.proposerName}의 회의 제안을 받았어요.",
                                    profileImageRes = com.example.maite.R.drawable.ic_launcher_foreground,
                                    meetingId = notification.meetingId,
                                    meetingDetails = MeetingDetails(
                                        title = notification.title,
                                        date = notification.meetingDate ?: "",
                                        time = notification.meetingTime ?: "",
                                        location = notification.address ?: ""
                                    )
                                )
                            )
                        }
                    }
                }
                
                _notifications.value = allNotifications
            } catch (e: Exception) {
                _error.value = "알림을 불러오는데 실패했습니다."
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun acceptRoomInvite(notificationId: Int) {
        viewModelScope.launch {
            val notification = _notifications.value?.find { it.id == notificationId }
            if (notification != null && notification.type == NotificationType.ROOM_INVITE && notification.roomId != null) {
                _isLoading.value = true
                
                // 실제 API 호출: ProposalRepository 사용
                val proposal = MeetingProposal(
                    id = notification.roomId,  // API에서는 roomId를 사용
                    type = ProposalType.ROOM_INVITE,
                    title = notification.senderName,  // 회의방 이름을 title로 사용
                    fromUser = "",  // hostEmail이 왔지만, fromUser로 매핑시 비워둡
                    roomId = notification.roomId,
                    roomName = notification.senderName
                )
                
                try {
                    val result = proposalRepository.acceptProposal(proposal)
                    result.onSuccess {
                        // 성공시 알림 목록에서 제거
                        _notifications.value = _notifications.value?.filterNot { it.id == notificationId }
                    }.onFailure { e ->
                        _error.value = "회의방 초대 수락에 실패했습니다: ${e.message}"
                    }
                } catch (e: Exception) {
                    _error.value = "회의방 초대 수락 중 오류가 발생했습니다"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun declineRoomInvite(notificationId: Int) {
        viewModelScope.launch {
            val notification = _notifications.value?.find { it.id == notificationId }
            if (notification != null && notification.type == NotificationType.ROOM_INVITE && notification.roomId != null) {
                _isLoading.value = true
                
                // 실제 API 호출: ProposalRepository 사용
                val proposal = MeetingProposal(
                    id = notification.roomId,  // API에서는 roomId를 사용
                    type = ProposalType.ROOM_INVITE,
                    title = notification.senderName,  // 회의방 이름을 title로 사용
                    fromUser = "",  // hostEmail이 왔지만, fromUser로 매핑시 비워둡
                    roomId = notification.roomId,
                    roomName = notification.senderName
                )
                
                try {
                    val result = proposalRepository.rejectProposal(proposal)
                    result.onSuccess {
                        // 성공시 알림 목록에서 제거
                        _notifications.value = _notifications.value?.filterNot { it.id == notificationId }
                    }.onFailure { e ->
                        _error.value = "회의방 초대 거절에 실패했습니다: ${e.message}"
                    }
                } catch (e: Exception) {
                    _error.value = "회의방 초대 거절 중 오류가 발생했습니다"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun acceptMeetingProposal(notificationId: Int) {
        viewModelScope.launch {
            val notification = _notifications.value?.find { it.id == notificationId }
            if (notification != null && notification.type == NotificationType.MEETING_INVITE && notification.meetingId != null) {
                _isLoading.value = true
                
                // 실제 API 호출: ProposalRepository 사용
                val proposal = MeetingProposal(
                    id = notification.meetingId,  // API에서는 meetingId를 사용
                    type = ProposalType.MEETING,
                    title = notification.meetingDetails?.title ?: "",
                    fromUser = notification.senderName,  // proposerName을 fromUser로 매핑
                    date = notification.meetingDetails?.date,
                    time = notification.meetingDetails?.time,
                    location = notification.meetingDetails?.location
                )
                
                try {
                    val result = proposalRepository.acceptProposal(proposal)
                    result.onSuccess {
                        // 성공시 알림 목록에서 제거
                        _notifications.value = _notifications.value?.filterNot { it.id == notificationId }
                    }.onFailure { e ->
                        _error.value = "회의 제안 수락에 실패했습니다: ${e.message}"
                    }
                } catch (e: Exception) {
                    _error.value = "회의 제안 수락 중 오류가 발생했습니다"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun declineMeetingProposal(notificationId: Int) {
        viewModelScope.launch {
            val notification = _notifications.value?.find { it.id == notificationId }
            if (notification != null && notification.type == NotificationType.MEETING_INVITE && notification.meetingId != null) {
                _isLoading.value = true
                
                // 실제 API 호출: ProposalRepository 사용
                val proposal = MeetingProposal(
                    id = notification.meetingId,  // API에서는 meetingId를 사용
                    type = ProposalType.MEETING,
                    title = notification.meetingDetails?.title ?: "",
                    fromUser = notification.senderName,  // proposerName을 fromUser로 매핑
                    date = notification.meetingDetails?.date,
                    time = notification.meetingDetails?.time,
                    location = notification.meetingDetails?.location
                )
                
                try {
                    val result = proposalRepository.rejectProposal(proposal)
                    result.onSuccess {
                        // 성공시 알림 목록에서 제거
                        _notifications.value = _notifications.value?.filterNot { it.id == notificationId }
                    }.onFailure { e ->
                        _error.value = "회의 제안 거절에 실패했습니다: ${e.message}"
                    }
                } catch (e: Exception) {
                    _error.value = "회의 제안 거절 중 오류가 발생했습니다"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
}

// 회의 상세 정보
data class MeetingDetails(
    val title: String,
    val date: String,
    val time: String,
    val location: String
)