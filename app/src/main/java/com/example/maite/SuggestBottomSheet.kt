package com.example.maite

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels // activityViewModels 사용
import androidx.lifecycle.Observer
import com.example.maite.databinding.BottomSheetSuggestBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.time.LocalDate
import java.time.format.DateTimeFormatter // 사용하지 않으면 제거 가능
import java.util.Locale // 사용하지 않으면 제거 가능

class SuggestBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSuggestBinding? = null
    private val binding get() = _binding!!

    // ViewModel 공유 (Activity 스코프)
    private val sharedViewModel: TimeSelectionViewModel by activityViewModels()

    // 사용 가능한 요일 목록 (Fragment Argument로 전달받음)
    private var availableDaysOfWeek: List<Int>? = null

    companion object {
        const val ARG_AVAILABLE_DAYS = "available_days" // Argument Key

        // newInstance 수정: 사용 가능한 요일 목록을 받도록 함
        fun newInstance(availableDays: ArrayList<Int>): SuggestBottomSheet {
            val fragment = SuggestBottomSheet()
            fragment.arguments = Bundle().apply {
                putIntegerArrayList(ARG_AVAILABLE_DAYS, availableDays)
            }
            Log.d("SuggestBottomSheet", "newInstance 호출됨, 전달된 요일: $availableDays")
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SuggestBottomSheet", "onCreate 호출됨")
        // Argument에서 사용 가능한 요일 목록 가져오기
        arguments?.let {
            availableDaysOfWeek = it.getIntegerArrayList(ARG_AVAILABLE_DAYS)
            Log.d("SuggestBottomSheet", "onCreate에서 Argument 로드, 사용 가능 요일: $availableDaysOfWeek")
        }
        if (availableDaysOfWeek == null) {
            Log.w("SuggestBottomSheet", "사용 가능한 요일 정보가 전달되지 않았습니다. 모든 요일을 허용합니다.")
            availableDaysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7) // 기본값: 모든 요일 허용
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("SuggestBottomSheet", "onCreateView 호출됨")
        _binding = BottomSheetSuggestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("SuggestBottomSheet", "onViewCreated 호출됨")

        setupObservers() // LiveData 관찰 설정

        // --- 버튼 리스너 설정 ---
        binding.title.setOnClickListener {
            // TODO: 제목 입력 로직 구현
            Toast.makeText(context, "제목 입력 로직 구현 필요", Toast.LENGTH_SHORT).show()
        }

        // dateBtn 클릭 시 DateBottomSheet 표시 (요일 정보 전달)
        binding.dateBtn.setOnClickListener {
            Log.d("SuggestBottomSheet", "dateBtn 클릭됨")
            availableDaysOfWeek?.let { days ->
                Log.d("SuggestBottomSheet", "DateBottomSheet 생성 시도, 전달 요일: $days")
                val datePicker = DateBottomSheet.newInstance(ArrayList(days))
                // parentFragmentManager 대신 childFragmentManager 사용 권장 (BottomSheet 내에서 다른 BottomSheet 호출 시)
                datePicker.show(childFragmentManager, "datePicker")
            } ?: run {
                Log.e("SuggestBottomSheet", "availableDaysOfWeek가 null이라 DateBottomSheet를 열 수 없습니다.")
                Toast.makeText(context, "요일 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // time1 버튼 리스너: 날짜 선택 여부 확인 추가
        binding.time1.setOnClickListener {
            if (sharedViewModel.getCurrentDate() == null) {
                Toast.makeText(context, "날짜를 먼저 선택해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentTime = parseTimeFromTextView(binding.time1Text)
            val timePicker = TimePickerBottomSheet.newInstance("time1", currentTime.first, currentTime.second)
            timePicker.show(childFragmentManager, "timePicker1")
        }

        // time2 버튼 리스너: 날짜 선택 여부 확인 추가
        binding.time2.setOnClickListener {
            if (sharedViewModel.getCurrentDate() == null) {
                Toast.makeText(context, "날짜를 먼저 선택해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 시작 시간이 설정되지 않았으면 종료 시간 선택 불가
            if (sharedViewModel.getCurrentStartTime() == null) {
                Toast.makeText(context, "시작 시간을 먼저 선택해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentTime = parseTimeFromTextView(binding.time2Text)
            val timePicker = TimePickerBottomSheet.newInstance("time2", currentTime.first, currentTime.second)
            timePicker.show(childFragmentManager, "timePicker2")
        }

        // 장소 선택 버튼 리스너
        binding.place.setOnClickListener {
            // TODO: 장소 선택 로직 구현
            Toast.makeText(context, "장소 선택 로직 구현 필요", Toast.LENGTH_SHORT).show()
        }

        // 완료 버튼 리스너
        binding.doneBtn.setOnClickListener {
            // 완료 버튼 비활성화 시 클릭 무시
            if (!binding.doneBtn.isEnabled) {
                Toast.makeText(context, "날짜와 유효한 시간을 모두 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val startTime = sharedViewModel.getCurrentStartTime()!! // null 체크는 isEnabled에서 이미 함
            val endTime = sharedViewModel.getCurrentEndTime()!! // null 체크는 isEnabled에서 이미 함
            val selectedDate = sharedViewModel.getCurrentDate()!! // null 체크는 isEnabled에서 이미 함

            // 최종 유효성 검사 (ViewModel 로직 사용 권장)
            // 여기서는 이미 버튼 활성화 로직에서 검사했다고 가정
            Log.i("SuggestBottomSheet", "회의 제안 완료: 날짜=$selectedDate, 시작=$startTime, 종료=$endTime")
            Toast.makeText(context, "회의 제안이 완료되었습니다", Toast.LENGTH_SHORT).show()

            // TODO: 실제 제안 로직 수행 (API 호출 등)

            dismiss() // BottomSheet 닫기
        }

        // 초기 상태 설정
        // LiveData observe가 초기값을 반영하므로, 여기서 별도 호출 불필요할 수 있음
        // checkAndUpdateDoneButtonState() // 필요 시 호출
        // updateDateText(sharedViewModel.getCurrentDate()) // 필요 시 호출
        // updateTimeText(binding.time1Text, sharedViewModel.getCurrentStartTime()) // 필요 시 호출
        // updateTimeText(binding.time2Text, sharedViewModel.getCurrentEndTime()) // 필요 시 호출
    }

    // LiveData 관찰 설정
    private fun setupObservers() {
        Log.d("SuggestBottomSheet", "setupObservers 호출됨")

        // 시작 시간 관찰
        sharedViewModel.startTime.observe(viewLifecycleOwner, Observer { startTimePair ->
            Log.d("SuggestBottomSheet", "시작 시간 LiveData 변경 감지: $startTimePair")
            updateTimeText(binding.time1Text, startTimePair)
            checkAndUpdateDoneButtonState() // 시작 시간 변경 시 완료 버튼 상태 재확인
        })

        // 종료 시간 관찰
        sharedViewModel.endTime.observe(viewLifecycleOwner, Observer { endTimePair ->
            Log.d("SuggestBottomSheet", "종료 시간 LiveData 변경 감지: $endTimePair")
            updateTimeText(binding.time2Text, endTimePair)
            checkAndUpdateDoneButtonState() // 종료 시간 변경 시 완료 버튼 상태 재확인
        })

        // 날짜 관찰
        sharedViewModel.selectedDate.observe(viewLifecycleOwner, Observer { date ->
            Log.d("SuggestBottomSheet", "날짜 LiveData 변경 감지: $date")
            updateDateText(date)
            // 날짜 변경 시 시간 선택 가능 여부가 달라질 수 있으므로 완료 버튼 상태 재확인
            checkAndUpdateDoneButtonState()
        })
    }

    // 날짜 텍스트 업데이트
    private fun updateDateText(date: LocalDate?) {
        if (_binding == null) {
            Log.w("SuggestBottomSheet", "updateDateText 호출 시 바인딩이 null입니다.")
            return
        }
        // ViewModel의 포맷 함수 사용
        binding.date.text = sharedViewModel.getFormattedDate()
    }

    // 시간 텍스트 업데이트
    private fun updateTimeText(textView: TextView, timePair: TimePair?) {
        if (_binding == null) {
            Log.w("SuggestBottomSheet", "updateTimeText 호출 시 바인딩이 null입니다.")
            return
        }
        textView.text = if (timePair != null) {
            String.format("%02d : %02d", timePair.first, timePair.second)
        } else {
            // 초기값 또는 null일 때 표시할 텍스트
            "시간 선택" // 또는 "00 : 00" 등
        }
    }

    // 완료 버튼 상태 업데이트 조건 확인
    private fun checkAndUpdateDoneButtonState() {
        if (_binding == null) {
            Log.w("SuggestBottomSheet", "checkAndUpdateDoneButtonState 호출 시 바인딩이 null입니다.")
            return
        }
        val startTime = sharedViewModel.getCurrentStartTime()
        val endTime = sharedViewModel.getCurrentEndTime()
        val selectedDate = sharedViewModel.getCurrentDate()

        // 모든 값이 null이 아니고, 종료 시간이 시작 시간보다 늦어야 함
        val isOverallValid = selectedDate != null && startTime != null && endTime != null &&
                sharedViewModel.isValidEndTime(endTime.first, endTime.second) // ViewModel의 유효성 검사 사용

        Log.d("SuggestBottomSheet", "checkAndUpdateDoneButtonState: Date=$selectedDate, Start=$startTime, End=$endTime, isOverallValid=$isOverallValid")
        updateDoneButtonStateVisuals(isOverallValid) // 시각적 업데이트 함수 호출
    }

    // 완료 버튼 시각/활성화 상태 업데이트
    private fun updateDoneButtonStateVisuals(isValid: Boolean) {
        if (_binding == null) {
            Log.w("SuggestBottomSheet", "updateDoneButtonStateVisuals 호출 시 바인딩이 null입니다.")
            return
        }
        binding.doneBtn.isEnabled = isValid
        binding.doneBtn.isClickable = isValid
        val context = context ?: return // context null 체크

        if (isValid) {
            binding.btnBg.setColorFilter(ContextCompat.getColor(context, R.color.mainColor))
            binding.btnText.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            binding.btnBg.setColorFilter(ContextCompat.getColor(context, R.color.btn_inactive))
            binding.btnText.setTextColor(ContextCompat.getColor(context, R.color.black)) // 또는 white/gray 등
        }
    }

    // TextView에서 시간 파싱 (오류 처리 강화)
    private fun parseTimeFromTextView(textView: TextView): Pair<Int, Int> {
        val timeString = textView.text.toString()
        if (!timeString.contains(":")) return Pair(0, 0) // 형식 안 맞으면 기본값 반환

        return try {
            val parts = timeString.split(":")
            if (parts.size == 2) {
                val hour = parts[0].trim().toIntOrNull()?.coerceIn(0, 23) ?: 0
                val minute = parts[1].trim().toIntOrNull()?.coerceIn(0, 59) ?: 0
                Pair(hour, minute)
            } else {
                Pair(0, 0)
            }
        } catch (e: Exception) {
            Log.e("SuggestBottomSheet", "시간 파싱 오류: '$timeString'", e)
            Pair(0, 0) // 오류 시 기본값 반환
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("SuggestBottomSheet", "onDestroyView 호출됨")
        _binding = null // 메모리 누수 방지
    }
}