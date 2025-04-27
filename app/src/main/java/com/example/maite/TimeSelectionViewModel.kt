package com.example.maite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// 시간 정보를 담는 간단한 Pair 사용 (또는 별도 Data Class 정의 가능)
typealias TimePair = Pair<Int, Int>

class TimeSelectionViewModel : ViewModel() {

    // 시작 시간 LiveData
    private val _startTime = MutableLiveData<TimePair?>() // Nullable로 초기 상태 표시
    val startTime: LiveData<TimePair?> = _startTime

    // 종료 시간 LiveData
    private val _endTime = MutableLiveData<TimePair?>() // Nullable로 초기 상태 표시
    val endTime: LiveData<TimePair?> = _endTime

    // 시작 시간 업데이트 함수
    fun updateStartTime(hour: Int, minute: Int) {
        _startTime.value = Pair(hour, minute)
    }

    // 종료 시간 업데이트 함수
    fun updateEndTime(hour: Int, minute: Int) {
        _endTime.value = Pair(hour, minute)
    }

    // 현재 시작 시간을 가져오는 함수 (TimePickerBottomSheet에서 사용)
    fun getCurrentStartTime(): TimePair? {
        return startTime.value // 현재 LiveData의 값 반환
    }

    // (선택적) 현재 종료 시간을 가져오는 함수
    fun getCurrentEndTime(): TimePair? {
        return endTime.value
    }
}