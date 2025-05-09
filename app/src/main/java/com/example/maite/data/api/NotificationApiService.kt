package com.example.maite.data.api

import com.example.maite.data.model.MeetingNotification
import com.example.maite.data.model.RoomNotification
import retrofit2.Response
import retrofit2.http.GET

interface NotificationApiService {
    
    @GET("notifications/rooms")
    suspend fun getRoomNotifications(): Response<List<RoomNotification>>
    
    @GET("notifications/meetings")
    suspend fun getMeetingNotifications(): Response<List<MeetingNotification>>
}
