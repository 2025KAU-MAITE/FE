package com.example.maite

import android.content.Context
import android.graphics.Color // Color import 유지 (혹시 직접 사용 대비)
// import android.graphics.drawable.ShapeDrawable // 배경 사용 안 하므로 제거 가능
// import android.graphics.drawable.shapes.OvalShape // 배경 사용 안 하므로 제거 가능
import android.os.Bundle
import android.text.style.ForegroundColorSpan // ForegroundColorSpan import 확인
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.example.maite.databinding.BottomSheetDateBinding // 실제 생성된 바인딩 클래스 이름 확인
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import java.time.LocalDate
import java.util.Locale

// ViewModel 클래스 import (실제 경로 확인)
import com.example.maite.TimeSelectionViewModel // 제공된 ViewModel 사용

class DateBottomSheet : BottomSheetDialogFragment(), OnDateSelectedListener {

    private var _binding: BottomSheetDateBinding? = null
    private val binding get() = _binding!!

    // 제공된 TimeSelectionViewModel 사용
    private val sharedViewModel: TimeSelectionViewModel by activityViewModels()

    private var availableDaysOfWeek: Set<Int>? = null // 1=월, ..., 7=일
    private var selectedDate: LocalDate? = null

    companion object {
        const val ARG_AVAILABLE_DAYS = "available_days"

        fun newInstance(availableDays: ArrayList<Int>): DateBottomSheet {
            return DateBottomSheet().apply {
                arguments = Bundle().apply {
                    putIntegerArrayList(ARG_AVAILABLE_DAYS, availableDays)
                }
                Log.d("DateBottomSheet", "newInstance 호출됨, 전달된 요일: $availableDays")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            availableDaysOfWeek = it.getIntegerArrayList(ARG_AVAILABLE_DAYS)?.toSet()
            Log.d("DateBottomSheet", "onCreate에서 Argument 로드, 사용 가능 요일: $availableDaysOfWeek")
        }
        // availableDaysOfWeek가 null이면 모든 요일 허용 (기본값)
        if (availableDaysOfWeek == null) {
            Log.w("DateBottomSheet", "사용 가능한 요일 정보 없음. 모든 요일 허용.")
            availableDaysOfWeek = setOf(1, 2, 3, 4, 5, 6, 7) // 월요일=1, ..., 일요일=7
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val today = CalendarDay.today()
        val context = requireContext() // Context 가져오기

        binding.calendarView.apply {
            // XML 레이아웃에서 app:mcv_selectionColor="@color/mainColor" 설정 확인 필요

            setOnDateChangedListener(this@DateBottomSheet)

            // --- Decorator 추가 ---
            // 1. 비활성화 날짜 Decorator (텍스트 색상 @color/gray)
            addDecorator(DisabledDayDecorator(context, availableDaysOfWeek ?: setOf(), today))
            // 2. 오늘 날짜 Decorator (텍스트 색상 @color/subColor)
            addDecorator(TodayDecorator(context))

            // ViewModel에서 날짜 복원 시도
            sharedViewModel.getCurrentDate()?.let { restoredLocalDate ->
                val restoredCalendarDay = CalendarDay.from(
                    restoredLocalDate.year,
                    restoredLocalDate.monthValue,
                    restoredLocalDate.dayOfMonth
                )
                // 복원 시에도 유효한 날짜인지 확인 (과거X, 선택 가능 요일O)
                if (!restoredCalendarDay.isBefore(today) &&
                    isDayAvailable(restoredCalendarDay, availableDaysOfWeek)) {
                    Log.d("DateBottomSheet", "ViewModel 날짜 복원 및 유효: $restoredLocalDate")
                    setDateSelected(restoredCalendarDay, true)
                    setCurrentDate(restoredCalendarDay, true) // 달력 페이지 이동
                    this@DateBottomSheet.selectedDate = restoredLocalDate
                    // updateDoneButtonState(true) // 아래에서 한 번에 처리
                } else {
                    Log.w("DateBottomSheet", "ViewModel 날짜 복원했으나 유효하지 않음: $restoredLocalDate")
                    // updateDoneButtonState(false) // 아래에서 한 번에 처리
                }
            } ?: run {
                Log.d("DateBottomSheet", "ViewModel에 복원할 날짜 없음")
                // updateDoneButtonState(false) // 아래에서 한 번에 처리
            }
        }

        // 완료 버튼 리스너
        binding.doneBtn.setOnClickListener {
            if (selectedDate != null) {
                Log.d("DateBottomSheet", "완료 버튼 클릭. 선택된 날짜: $selectedDate")
                sharedViewModel.updateSelectedDate(selectedDate!!) // ViewModel 업데이트
                dismiss() // BottomSheet 닫기
            } else {
                Log.w("DateBottomSheet", "완료 버튼 클릭했으나 선택된 날짜 없음")
                Toast.makeText(context, "날짜를 선택해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        // 초기 버튼 상태 업데이트 (ViewModel 복원 후 최종 상태 반영)
        updateDoneButtonState(selectedDate != null)
    }

    // --- OnDateSelectedListener 구현 ---
    override fun onDateSelected(
        widget: MaterialCalendarView,
        date: CalendarDay,
        selected: Boolean
    ) {
        // --- 클릭 방지 로직 ---
        if (date.isBefore(CalendarDay.today()) || !isDayAvailable(date, availableDaysOfWeek)) {
            Log.d("DateBottomSheet", "비활성화된 날짜 클릭됨 (무시): $date")
            if (selected) {
                widget.setDateSelected(date, false) // 선택 강제 해제
            }
            // 상태 변경 없이 종료
            return
        }
        // --- 여기까지 클릭 방지 로직 ---

        Log.d("DateBottomSheet", "onDateSelected (유효한 날짜): Date=$date, Selected=$selected")
        val selectedLocalDate = date.toLocalDate()

        if (selected) {
            // 유효한 날짜가 '선택'되었을 때
            Log.d("DateBottomSheet", "선택된 날짜 유효: $selectedLocalDate")
            this.selectedDate = selectedLocalDate
            updateDoneButtonState(true) // 완료 버튼 활성화
        } else {
            // 유효한 날짜가 '선택 해제'되었을 때 (Single 모드에서는 거의 발생 안 함)
            Log.d("DateBottomSheet", "날짜 선택 해제됨: $selectedLocalDate")
            if (this.selectedDate == selectedLocalDate) {
                this.selectedDate = null
                updateDoneButtonState(false) // 완료 버튼 비활성화
            }
        }
    }

    // 완료 버튼 상태 업데이트 (colors.xml 사용 확인)
    private fun updateDoneButtonState(isEnabled: Boolean) {
        if (_binding == null) return
        binding.doneBtn.isEnabled = isEnabled
        binding.doneBtn.isClickable = isEnabled
        val context = context ?: return

        if (isEnabled) {
            // 활성화: 배경 mainColor, 텍스트 white
            binding.btnBg.setColorFilter(ContextCompat.getColor(context, R.color.mainColor))
            binding.btnText.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            // 비활성화: 배경 btn_inactive, 텍스트 black (디자인에 맞게 수정 가능)
            binding.btnBg.setColorFilter(ContextCompat.getColor(context, R.color.btn_inactive))
            binding.btnText.setTextColor(ContextCompat.getColor(context, R.color.black)) // 필요시 white 또는 gray 등으로 변경
        }
        Log.d("DateBottomSheet", "완료 버튼 상태 업데이트: isEnabled=$isEnabled")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
        Log.d("DateBottomSheet", "onDestroyView 호출됨")
    }

    // --- Helper Functions ---

    // CalendarDay를 LocalDate로 변환
    private fun CalendarDay.toLocalDate(): LocalDate {
        return LocalDate.of(this.year, this.month, this.day)
    }

    // 특정 날짜(CalendarDay)가 사용 가능한 요일인지 확인
    private fun isDayAvailable(date: CalendarDay, availableDays: Set<Int>?): Boolean {
        if (availableDays == null) return true // 정보 없으면 항상 true
        val localDate = date.toLocalDate()
        val isoDayOfWeek = localDate.dayOfWeek.value // 월=1, ..., 일=7
        return availableDays.contains(isoDayOfWeek)
    }

    // --- 비활성화 날짜 Decorator (@color/gray 텍스트 색상) ---
    private class DisabledDayDecorator(
        context: Context, // Context 추가
        private val availableDays: Set<Int>,
        private val today: CalendarDay
    ) : DayViewDecorator {
        // colors.xml의 gray 색상 사용
        private val disabledTextColor = ContextCompat.getColor(context, R.color.gray)

        override fun shouldDecorate(day: CalendarDay): Boolean {
            // 오늘 이전 날짜
            if (day.isBefore(today)) {
                return true
            }
            // 사용 불가능한 요일
            val localDate = day.toLocalDate()
            val isoDayOfWeek = localDate.dayOfWeek.value
            return !availableDays.contains(isoDayOfWeek)
        }
        override fun decorate(view: DayViewFacade) {
            view.addSpan(ForegroundColorSpan(disabledTextColor)) // 비활성화 텍스트 색상 적용
        }
        // Helper function (중복 제거 가능하나 편의상 유지)
        private fun CalendarDay.toLocalDate(): LocalDate {
            return LocalDate.of(this.year, this.month, this.day)
        }
    }


    // --- 오늘 날짜 표시를 위한 Decorator (텍스트 색상 @color/subColor) ---
    private class TodayDecorator(context: Context) : DayViewDecorator {
        private val today = CalendarDay.today()
        // colors.xml의 subColor 사용
        private val todayTextColor = ContextCompat.getColor(context, R.color.mainColor)

        override fun shouldDecorate(day: CalendarDay): Boolean {
            return day == today // 오늘 날짜만
        }

        override fun decorate(view: DayViewFacade) {
            // 오늘 날짜 텍스트 색상 적용
            view.addSpan(ForegroundColorSpan(todayTextColor))
        }
    }
}