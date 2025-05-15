package com.example.maite.data.repository

import android.util.Log
import com.example.maite.R
import com.example.maite.data.api.NotificationApiService
import com.example.maite.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationRepository(
    private val apiService: NotificationApiService
) {
    private val TAG = "NotificationRepository"

    /**
     * 모든 알림 가져오기 (회의방 초대 + 회의 제안 + 친구 요청)
     */
    suspend fun getAllNotifications(): Result<List<NotificationItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val notifications = mutableListOf<NotificationItem>()
                
                // 1. 회의방 초대 알림 가져오기
                val roomResponse = apiService.getRoomNotifications()
                if (roomResponse.isSuccessful) {
                    roomResponse.body()?.let { roomNotifications ->
                        Log.d(TAG, "회의방 초대 알림: ${roomNotifications.size}개")
                        
                        notifications.addAll(
                            roomNotifications.mapIndexed { index, room ->
                                NotificationItem(
                                    id = room.roomId,
                                    type = NotificationType.ROOM_INVITE,
                                    senderName = room.hostEmail.substringBefore('@'),  // 이메일에서 이름 추출
                                    message = "${room.name}에서 초대를 받았어요.",
                                    profileImageRes = R.drawable.ic_launcher_foreground,
                                    originalData = room
                                )
                            }
                        )
                    }
                } else {
                    Log.e(TAG, "회의방 초대 알림 실패: ${roomResponse.code()}")
                }
                
                // 2. 회의 제안 알림 가져오기
                val meetingResponse = apiService.getMeetingNotifications()
                if (meetingResponse.isSuccessful) {
                    meetingResponse.body()?.let { meetingNotifications ->
                        Log.d(TAG, "회의 제안 알림: ${meetingNotifications.size}개")
                        
                        notifications.addAll(
                            meetingNotifications.mapIndexed { index, meeting ->
                                NotificationItem(
                                    id = meeting.meetingId.toInt(),
                                    type = NotificationType.MEETING_INVITE,
                                    senderName = meeting.proposerName,
                                    message = "${meeting.title}에 대한 제안을 받았어요.",
                                    profileImageRes = R.drawable.ic_launcher_foreground,
                                    originalData = meeting
                                )
                            }
                        )
                    }
                } else {
                    Log.e(TAG, "회의 제안 알림 실패: ${meetingResponse.code()}")
                }
                
                // 3. 친구 요청 알림 (현재 API 없으므로 더미 데이터)
                // TODO: 실제 친구 요청 API가 추가되면 실제 데이터로 대체
                if (notifications.isEmpty()) {  // 테스트를 위해 알림이 없을 때만 더미 데이터 추가
                    notifications.add(
                        NotificationItem(
                            id = 999,
                            type = NotificationType.FRIEND_REQUEST,
                            senderName = "김정훈",
                            message = "김정훈님이 친구 요청을 보냈습니다.",
                            profileImageRes = R.drawable.ic_launcher_foreground
                        )
                    )
                }
                
                Log.d(TAG, "전체 알림: ${notifications.size}개")
                Result.success(notifications)
                
            } catch (e: Exception) {
                Log.e(TAG, "알림 가져오기 실패", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * 회의방 초대 알림만 가져오기
     */
    suspend fun getRoomNotifications(): Result<List<RoomNotification>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRoomNotifications()
                if (response.isSuccessful) {
                    response.body()?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("Response body is null"))
                } else {
                    Result.failure(Exception("Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 회의 제안 알림만 가져오기
     */
    suspend fun getMeetingNotifications(): Result<List<MeetingNotification>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMeetingNotifications()
                if (response.isSuccessful) {
                    response.body()?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("Response body is null"))
                } else {
                    Result.failure(Exception("Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
