package com.example.maite

import com.example.maite.data.model.MeetingItem
import retrofit2.http.GET

interface MeetingApi {

    // ✅ 회의 목록 가져오기 (GET /rooms)
    @GET("rooms")
    suspend fun getMyMeetings(): List<MeetingItem>
}
