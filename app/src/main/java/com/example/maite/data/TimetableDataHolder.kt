package com.example.maite.data

import android.util.Log
import com.example.maite.model.TimetableEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 시간표 데이터를 여러 ViewModel 간에 공유하기 위한 싱글톤 클래스
 * 서버 중심의 시간표 관리를 위해 로컬 캐싱 기능을 최소화
 */
object TimetableDataHolder {
    private val TAG = "TimetableDataHolder"
    private val _timetableEntries = MutableStateFlow<List<TimetableEntry>>(emptyList())
    val timetableEntries: StateFlow<List<TimetableEntry>> = _timetableEntries.asStateFlow()

    fun updateTimetable(entries: List<TimetableEntry>) {
        // 서버에서 받은 데이터만 업데이트 (로컬 우선순위 제거)
        Log.d(TAG, "시간표 업데이트 (서버 중심): ${entries.size}개 항목")
        _timetableEntries.value = entries
    }
    
    // 시간표 초기화
    fun clear() {
        Log.d(TAG, "시간표 초기화")
        _timetableEntries.value = emptyList()
    }
    
    // 현재 시간표 항목 개수 가져오기
    fun getEntriesCount(): Int {
        return _timetableEntries.value.size
    }
    
    // 임시 데이터 확인용 (서버와의 동기화 확인)
    fun getCurrentEntries(): List<TimetableEntry> {
        return _timetableEntries.value
    }
}