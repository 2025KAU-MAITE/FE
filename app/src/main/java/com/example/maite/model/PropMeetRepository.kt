package com.example.maite.model

import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList

class PropMeetRepository private constructor() {
    // 스레드 안전한 리스트 사용
    private val propItems = CopyOnWriteArrayList<PropMeetItem>()

    // 현재 제안된 회의 목록 반환
    fun getProposedMeetings(): List<PropMeetItem> {
        Log.d("PropMeetRepository", "getProposedMeetings 호출: ${propItems.size}개 항목")
        return propItems.toList()
    }

    // 단일 제안 추가
    fun addProposal(item: PropMeetItem) {
        propItems.add(item)
        Log.d("PropMeetRepository", "addProposal: ${item.title}, 현재 ${propItems.size}개 항목")
    }

    // 여러 제안 한번에 추가
    fun addAllProposals(items: List<PropMeetItem>) {
        propItems.addAll(items)
        Log.d("PropMeetRepository", "addAllProposals: ${items.size}개 항목 추가, 현재 ${propItems.size}개 항목")
    }

    // 모든 제안 삭제
    fun clearProposals() {
        propItems.clear()
        Log.d("PropMeetRepository", "clearProposals: 모든 항목 삭제됨")
    }

    // 싱글톤 구현
    companion object {
        @Volatile
        private var instance: PropMeetRepository? = null

        fun getInstance(): PropMeetRepository {
            return instance ?: synchronized(this) {
                instance ?: PropMeetRepository().also { instance = it }
            }
        }
    }
}