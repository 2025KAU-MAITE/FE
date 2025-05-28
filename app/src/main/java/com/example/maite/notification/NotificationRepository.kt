package com.example.maite.notification

import android.util.Log

class NotificationRepository(
    private val api: NotificationApiService
) {
    private val TAG = "NotificationRepository"
    
    suspend fun getRoomInviteNotifications(): Result<List<RoomInviteNotification>> {
        return try {
            val response = api.getRoomInviteNotifications()
            if (response.isSuccessful) {
                val notifications = response.body() ?: emptyList()
                Log.d(TAG, "회의방 초대 알림 조회 성공: ${notifications.size}개")
                Result.success(notifications)
            } else {
                Log.e(TAG, "회의방 초대 알림 조회 실패: ${response.code()}")
                Result.failure(Exception("Failed to get room invite notifications: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "회의방 초대 알림 조회 중 오류", e)
            Result.failure(e)
        }
    }
    
    suspend fun getMeetingNotifications(): Result<List<MeetingNotification>> {
        return try {
            val response = api.getMeetingNotifications()
            if (response.isSuccessful) {
                val notifications = response.body() ?: emptyList()
                Log.d(TAG, "회의 제안 알림 조회 성공: ${notifications.size}개")
                Result.success(notifications)
            } else {
                Log.e(TAG, "회의 제안 알림 조회 실패: ${response.code()}")
                Result.failure(Exception("Failed to get meeting notifications: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "회의 제안 알림 조회 중 오류", e)
            Result.failure(e)
        }
    }
    
    suspend fun getFriendRequestNotifications(): Result<List<FriendRequestNotification>> {
        return try {
            val response = api.getFriendRequestNotifications()
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.isSuccess == true) {
                    val notifications = apiResponse.result ?: emptyList()
                    Log.d(TAG, "친구 요청 알림 조회 성공: ${notifications.size}개")
                    Result.success(notifications)
                } else {
                    Log.e(TAG, "친구 요청 알림 조회 실패: ${apiResponse?.message}")
                    Result.failure(Exception("API Error: ${apiResponse?.message}"))
                }
            } else {
                Log.e(TAG, "친구 요청 알림 조회 실패: ${response.code()}")
                Result.failure(Exception("Failed to get friend request notifications: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "친구 요청 알림 조회 중 오류", e)
            Result.failure(e)
        }
    }
}