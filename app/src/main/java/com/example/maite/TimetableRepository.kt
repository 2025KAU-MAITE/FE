package com.example.maite

import com.example.maite.TimetableApi
import com.example.maite.model.EventRequest
import com.example.maite.model.TimetableEntry
import com.example.maite.model.network.TimetableRequest
import com.example.maite.model.network.TimetableResponse
import com.example.maite.model.Event
import com.example.maite.util.mapper.toEventRequest
import com.example.maite.util.mapper.toTimetableEntry
import retrofit2.Response

class TimetableRepository(
    private val api: TimetableApi
) {

    suspend fun createTimetable(userId: Long): Response<TimetableResponse> {
        val request = TimetableRequest(userId = userId)
        return api.createTimetable(request)
    }

    suspend fun saveTimetable(timetableId: Long, entries: List<TimetableEntry>) {
        for (entry in entries) {
            val event = entry.toEventRequest()
            api.postTimetableEvent(timetableId, event)
        }
    }

    // 🆕 서버에서 시간표 불러오기
    suspend fun loadTimetable(userId: Long): List<TimetableEntry> {
        val response = api.getTimetableByUserId(userId)
        if (response.isSuccessful) {
            return response.body()?.map { it.toTimetableEntry() } ?: emptyList()
        } else {
            throw Exception("서버에서 시간표 불러오기 실패: ${response.message()}")
        }
    }
}
