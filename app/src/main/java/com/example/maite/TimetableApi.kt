package com.example.maite

import com.example.maite.model.network.TimetableRequest
import com.example.maite.model.network.TimetableResponse
import com.example.maite.model.EventRequest
import com.example.maite.model.Event
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.GET

interface TimetableApi {

    // 시간표 생성
    @POST("/api/timetables")
    suspend fun createTimetable(
        @Body request: TimetableRequest
    ): Response<TimetableResponse>

    // 시간표에 개별 이벤트 추가
    @POST("/api/timetables/{timetableId}/event")
    suspend fun postTimetableEvent(
        @Path("timetableId") timetableId: Long,
        @Body event: EventRequest
    ): Response<Event>

    @GET("/api/timetables/user/{userId}")
    suspend fun getTimetableByUserId(
        @Path("userId") userId: Long
    ): Response<List<Event>>

}
