package com.example.maite

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

typealias TimePair = Pair<Int, Int>
typealias TimeSlotMinutes = Pair<Int, Int> // 시작 분, 종료 분 (분 단위, 0~1439)

data class TimetableItemModel(
    val timeSlot: Int,
    val dayOfWeek: Int,
    val className: String,
    val color: Int
)

class TimeSelectionViewModel : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd.EEEE", Locale.KOREAN)

    private val _startTime = MutableLiveData<TimePair?>()
    val startTime: LiveData<TimePair?> = _startTime

    private val _endTime = MutableLiveData<TimePair?>()
    val endTime: LiveData<TimePair?> = _endTime

    private val _selectedDate = MutableLiveData<LocalDate?>()
    val selectedDate: LiveData<LocalDate?> = _selectedDate

    private val _timetableData = MutableLiveData<List<TimetableItemModel>>()

    fun setTimetableData(data: List<ListDetailFragment.TimetableItem>) {
        _timetableData.value = data.map { TimetableItemModel(it.timeSlot, it.dayOfWeek, it.className, it.color) }
        Log.d("ViewModel", "시간표 데이터 설정됨: ${_timetableData.value?.size ?: 0}개 항목")
    }

    fun updateStartTime(hour: Int, minute: Int) { _startTime.value = Pair(hour, minute) }
    fun updateEndTime(hour: Int, minute: Int) { _endTime.value = Pair(hour, minute) }
    fun updateSelectedDate(date: LocalDate) { _selectedDate.value = date }

    fun getCurrentStartTime(): TimePair? = startTime.value
    fun getCurrentEndTime(): TimePair? = endTime.value
    fun getCurrentDate(): LocalDate? = selectedDate.value
    fun getFormattedDate(): String = selectedDate.value?.format(dateFormatter) ?: "날짜 선택하기"

    // --- 시간 유효성 검사 로직 (수정됨) ---

    /**
     * 선택된 날짜의 요일에 해당하는 "사용 가능한 시간 슬롯" 목록을 분 단위로 반환
     * (시작 분 포함, 종료 분 미포함) - 기존 getBusySlots와 로직 동일, 이름만 변경
     * 예: 10:00~13:00 수업 -> [600, 780) 반환
     */
    private fun getAvailableSlotsForSelectedDate(): List<TimeSlotMinutes> {
        val date = _selectedDate.value ?: return emptyList()
        val currentTimetable = _timetableData.value ?: return emptyList()
        val dayOfWeekIso = date.dayOfWeek.value

        val availableSlots = currentTimetable
            .filter { it.dayOfWeek == dayOfWeekIso }
            .map {
                val startMinute = it.timeSlot * 60
                val endMinute = startMinute + 60 // 1시간 단위 가정
                Pair(startMinute, endMinute)
            }
            .sortedBy { it.first }

        // 병합 로직 (연속된 슬롯 합치기)
        val mergedSlots = mutableListOf<TimeSlotMinutes>()
        for (slot in availableSlots) {
            if (mergedSlots.isEmpty() || mergedSlots.last().second < slot.first) {
                mergedSlots.add(slot)
            } else {
                val lastSlot = mergedSlots.removeLast()
                mergedSlots.add(Pair(lastSlot.first, max(lastSlot.second, slot.second)))
            }
        }
        // Log.d("ViewModel", "선택된 날짜($dayOfWeekIso)의 사용 가능 시간: $mergedSlots") // 필요시 로그 활성화
        return mergedSlots
    }

    /**
     * 주어진 시간(시, 분)이 "사용 가능한 시간 슬롯" 내에 있는지 확인
     * (시작 시간 포함, 종료 시간 미포함) - 기존 isTimeInBusySlot과 로직 동일, 이름만 변경
     */
    private fun isTimeInAvailableSlot(hour: Int, minute: Int): Boolean {
        val targetMinute = hour * 60 + minute
        val availableSlots = getAvailableSlotsForSelectedDate()
        return availableSlots.any { targetMinute >= it.first && targetMinute < it.second }
    }

    /**
     * 시작 시간(time1)으로 유효한지 확인 (수정됨)
     * - 사용 가능한 시간 슬롯 내에 있어야 함
     * - 해당 슬롯 내에서 종료 시간을 위한 최소 5분 간격이 남아 있어야 함
     */
    fun isValidStartTime(hour: Int, minute: Int): Boolean {
        val targetMinute = hour * 60 + minute
        val availableSlots = getAvailableSlotsForSelectedDate()

        // 1. 시간이 속한 사용 가능 슬롯 찾기
        val containingSlot = availableSlots.find { targetMinute >= it.first && targetMinute < it.second }

        // 2. 사용 가능 슬롯이 없으면 유효하지 않음
        if (containingSlot == null) {
            Log.d("ViewModel-Validation", "시작 시간 $hour:$minute 유효성: 실패 (사용 가능 시간 아님)")
            return false
        }

        // 3. 해당 슬롯 내에서 종료 시간을 위한 5분 이상 남아 있는지 확인
        val remainingDuration = containingSlot.second - targetMinute
        val isValid = remainingDuration >= 5

        Log.d("ViewModel-Validation", "시작 시간 $hour:$minute 유효성: 성공여부=$isValid (슬롯 $containingSlot 내 남은시간=${remainingDuration}분)")
        return isValid
    }

    /**
     * 종료 시간(time2)으로 유효한지 확인 (수정됨)
     * - 시작 시간보다 늦어야 함 (최소 5분)
     * - 시작 시간과 종료 시간이 *동일한* 사용 가능 시간 슬롯 내에 있어야 함
     */
    fun isValidEndTime(endHour: Int, endMinute: Int): Boolean {
        val startTime = _startTime.value ?: return false // 시작 시간이 없으면 비교 불가
        val startTotalMinutes = startTime.first * 60 + startTime.second
        val endTotalMinutes = endHour * 60 + endMinute

        // 1. 종료 시간이 시작 시간보다 최소 5분 늦어야 함
        if (endTotalMinutes <= startTotalMinutes) {
            Log.d("ViewModel-Validation", "종료 시간 $endHour:$endMinute 유효성: 실패 (시작 시간보다 빠르거나 같음)")
            return false
        }

        val availableSlots = getAvailableSlotsForSelectedDate()

        // 2. 시작 시간이 속한 사용 가능 슬롯 찾기
        val containingSlot = availableSlots.find { startTotalMinutes >= it.first && startTotalMinutes < it.second }

        // 3. 시작 시간이 사용 가능 슬롯에 없으면 유효하지 않음 (이론상 발생 안 함)
        if (containingSlot == null) {
            Log.e("ViewModel-Validation", "종료 시간 유효성 검사 중 오류: 시작 시간이 사용 가능 슬롯에 없음 ($startTime)")
            return false
        }

        // 4. 종료 시간이 시작 시간과 *동일한* 사용 가능 슬롯 내에 있는지 확인
        // 종료 시간은 슬롯의 끝 시간과 같아도 됨 (예: 13:00까지 가능하면 13:00 선택 가능)
        val isEndTimeInSameSlot = endTotalMinutes > containingSlot.first && endTotalMinutes <= containingSlot.second

        if (!isEndTimeInSameSlot) {
            Log.d("ViewModel-Validation", "종료 시간 $endHour:$endMinute 유효성: 실패 (시작 시간과 동일한 사용 가능 슬롯 $containingSlot 에 속하지 않음)")
            return false
        }

        Log.d("ViewModel-Validation", "종료 시간 $endHour:$endMinute 유효성: 성공 (슬롯 $containingSlot 내)")
        return true // 모든 검사를 통과하면 유효
    }
}