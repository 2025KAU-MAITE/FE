package com.example.maite.model

import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

class MeetListRepository private constructor() {
    // 스레드 안전한 리스트 사용
    private val meetItems = CopyOnWriteArrayList<MeetListItem>()

    // 현재 회의 목록 반환
    fun getMeetList(): List<MeetListItem> {
        Log.d("MeetListRepository", "getMeetList 호출: ${meetItems.size}개 항목")
        return meetItems.toList()
    }

    // 단일 회의 추가
    fun addMeeting(item: MeetListItem) {
        meetItems.add(item)
        Log.d("MeetListRepository", "addMeeting: ${item.title}, 현재 ${meetItems.size}개 항목")
    }

    // 여러 회의 한번에 추가
    fun addAllMeetings(items: List<MeetListItem>) {
        meetItems.addAll(items)
        Log.d("MeetListRepository", "addAllMeetings: ${items.size}개 항목 추가, 현재 ${meetItems.size}개 항목")
    }

    // 모든 회의 삭제
    fun clearMeetings() {
        meetItems.clear()
        Log.d("MeetListRepository", "clearMeetings: 모든 항목 삭제됨")
    }

    // 싱글톤 구현
    companion object {
        @Volatile
        private var instance: MeetListRepository? = null

        fun getInstance(): MeetListRepository {
            return instance ?: synchronized(this) {
                instance ?: MeetListRepository().also { instance = it }
            }
        }
    }
}