package com.example.maite.data

import com.example.maite.model.TimetableEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 시간표 데이터를 여러 ViewModel 간에 공유하기 위한 싱글톤 클래스
 */
object TimetableDataHolder {
    private val _timetableEntries = MutableStateFlow<List<TimetableEntry>>(emptyList())
    val timetableEntries: StateFlow<List<TimetableEntry>> = _timetableEntries.asStateFlow()

    fun updateTimetable(entries: List<TimetableEntry>) {
        _timetableEntries.value = entries
    }
}