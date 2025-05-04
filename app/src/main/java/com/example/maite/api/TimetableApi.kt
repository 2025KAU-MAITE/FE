package com.example.maite.api

import retrofit2.Response
import retrofit2.http.*

interface TimetableApi {

    @POST("api/timetables")
    suspend fun createTimetable(@Body request: CreateTimetableRequest): Response<TimetableResponse>

    @GET("api/timetables/{timetableId}")
    suspend fun getTimetable(@Path("timetableId") timetableId: Long): Response<TimetableResponse>

    @DELETE("api/timetables/{timetableId}")
    suspend fun deleteTimetable(@Path("timetableId") timetableId: Long): Response<TimetableResponse>

    @POST("api/timetables/{timetableId}/event")
    suspend fun createEvent(
        @Path("timetableId") timetableId: Long,
        @Body request: CreateEventRequest
    ): Response<EventResponse>

    @GET("api/timetables/{timetableId}/events")
    suspend fun getAllEvents(
        @Path("timetableId") timetableId: Long
    ): Response<EventsResponse>

    @DELETE("api/timetables/{timetableId}/event/{eventId}")
    suspend fun deleteEvent(
        @Path("timetableId") timetableId: Long,
        @Path("eventId") eventId: Long
    ): Response<EventResponse>
}

data class CreateTimetableRequest(
    val title: String,
    val userId: Long? = null
)

data class TimetableResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: TimetableResult
)

data class TimetableResult(
    val id: Long,
    val userId: Long,
    val events: List<EventDto>
)

data class EventDto(
    val id: Long,
    val title: String,
    val day: String,
    val color: String?,  // nullable로 변경
    val startTime: String,
    val endTime: String
)

data class CreateEventRequest(
    val title: String,
    val day: String,
    val color: String,
    val startTime: String,
    val endTime: String
)

data class EventResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: EventDto
)

data class EventsResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: List<EventDto>
)