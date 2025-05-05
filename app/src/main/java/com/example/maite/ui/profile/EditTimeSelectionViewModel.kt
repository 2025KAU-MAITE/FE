package com.example.maite.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate

// 시간 쌍(시, 분)을 나타내는 타입 별칭
typealias EditTimePair = Pair<Int, Int>

class EditTimeSelectionViewModel : ViewModel() {

    private val _startTime = MutableLiveData<EditTimePair?>()
    val startTime: LiveData<EditTimePair?> = _startTime

    private val _endTime = MutableLiveData<EditTimePair?>()
    val endTime: LiveData<EditTimePair?> = _endTime

    private val _selectedDate = MutableLiveData<LocalDate?>()
    val selectedDate: LiveData<LocalDate?> = _selectedDate

    // 기본값 설정
    init {
        // 초기 시작 시간: 09:00
        _startTime.value = Pair(9, 0)
        // 초기 종료 시간: 10:00
        _endTime.value = Pair(10, 0)
        // 오늘 날짜 설정
        _selectedDate.value = LocalDate.now()
    }

    // 사용자가 선택한 시간 업데이트 메서드
    fun updateStartTime(hour: Int, minute: Int) {
        _startTime.value = Pair(hour, minute)
    }

    fun updateEndTime(hour: Int, minute: Int) {
        _endTime.value = Pair(hour, minute)
    }

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    // 현재 선택된 값 가져오기
    fun getCurrentStartTime(): EditTimePair? = startTime.value
    fun getCurrentEndTime(): EditTimePair? = endTime.value
    fun getCurrentDate(): LocalDate? = selectedDate.value
}