package com.example.maite.data

import android.util.Log
import com.example.maite.model.TimetableEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 시간표 데이터를 여러 ViewModel 간에 공유하기 위한 싱글톤 클래스
 */
object TimetableDataHolder {
    private val TAG = "TimetableDataHolder"
    private val _timetableEntries = MutableStateFlow<List<TimetableEntry>>(emptyList())
    val timetableEntries: StateFlow<List<TimetableEntry>> = _timetableEntries.asStateFlow()

    fun updateTimetable(entries: List<TimetableEntry>) {
        // 영향업데이트 방지: 동일한 데이터라면 불필요한 업데이트 스키입
        val currentEntries = _timetableEntries.value
        if (currentEntries.size == entries.size && 
            currentEntries.containsAll(entries) && 
            entries.containsAll(currentEntries)) {
            Log.d(TAG, "동일한 데이터이므로 업데이트 스키옭니다: ${entries.size}개 항목")
            return
        }
        
        Log.d(TAG, "시간표 업데이트: ${entries.size}개 항목")
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
}