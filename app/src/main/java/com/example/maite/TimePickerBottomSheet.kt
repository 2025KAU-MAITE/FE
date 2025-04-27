package com.example.maite

import android.graphics.Color // Color는 이제 직접 사용 안 함
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Toast
import androidx.core.content.ContextCompat // ContextCompat 임포트 추가
import androidx.fragment.app.viewModels
import com.example.maite.databinding.BottomSheetTimePickerBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TimePickerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTimePickerBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: TimeSelectionViewModel by viewModels({ requireParentFragment() })

    private var targetTimeView: String = "time1"

    companion object {
        const val KEY_TARGET_TIME_VIEW = "targetTimeView"
        const val KEY_INITIAL_HOUR = "initialHour"
        const val KEY_INITIAL_MINUTE = "initialMinute"

        fun newInstance(targetTimeView: String, initialHour: Int, initialMinute: Int): TimePickerBottomSheet {
            val fragment = TimePickerBottomSheet()
            fragment.arguments = Bundle().apply {
                putString(KEY_TARGET_TIME_VIEW, targetTimeView)
                putInt(KEY_INITIAL_HOUR, initialHour)
                putInt(KEY_INITIAL_MINUTE, initialMinute)
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomSheetTimePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        targetTimeView = arguments?.getString(KEY_TARGET_TIME_VIEW) ?: "time1"

        // --- 초기값 설정 ---
        val initialHour = arguments?.getInt(KEY_INITIAL_HOUR, 0) ?: 0
        val initialMinute = arguments?.getInt(KEY_INITIAL_MINUTE, 0) ?: 0
        binding.hourPicker.minValue = 0
        binding.hourPicker.maxValue = 23
        binding.hourPicker.setFormatter { String.format("%02d", it) }
        binding.hourPicker.value = initialHour
        binding.minutePicker.minValue = 0
        binding.minutePicker.maxValue = 11
        val minuteValues = Array(12) { String.format("%02d", it * 5) }
        binding.minutePicker.displayedValues = minuteValues
        val initialMinuteIndex = (initialMinute + 2) / 5
        binding.minutePicker.value = initialMinuteIndex.coerceIn(0, 11)

        // --- NumberPicker 리스너 설정 ---
        val valueChangeListener = NumberPicker.OnValueChangeListener { _, _, _ ->
            checkAndUpdateDoneButtonState()
        }
        binding.hourPicker.setOnValueChangedListener(valueChangeListener)
        binding.minutePicker.setOnValueChangedListener(valueChangeListener)

        // --- 완료 버튼 클릭 리스너 ---
        binding.doneBtn.setOnClickListener {
            if (!binding.doneBtn.isEnabled) {
                return@setOnClickListener
            }

            val selectedHour = binding.hourPicker.value
            val selectedMinute = binding.minutePicker.value * 5

            if (targetTimeView == "time2") {
                val currentStartTime = sharedViewModel.getCurrentStartTime()
                if (currentStartTime != null) {
                    val startTimeInMinutes = currentStartTime.first * 60 + currentStartTime.second
                    val selectedEndTimeInMinutes = selectedHour * 60 + selectedMinute
                    if (selectedEndTimeInMinutes <= startTimeInMinutes) {
                        Log.e("TimePicker", "오류: 비활성화된 버튼이 클릭됨 또는 로직 오류")
                        Toast.makeText(context, "종료 시간은 시작 시간보다 늦어야 합니다.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
                Log.d("TimePicker", "ViewModel 종료 시간 업데이트: $selectedHour:$selectedMinute")
                sharedViewModel.updateEndTime(selectedHour, selectedMinute)
            } else {
                Log.d("TimePicker", "ViewModel 시작 시간 업데이트: $selectedHour:$selectedMinute")
                sharedViewModel.updateStartTime(selectedHour, selectedMinute)
            }

            Log.d("TimePicker", "ViewModel 업데이트 후 dismiss 호출")
            dismiss()
        }
        binding.btnText.text = "선택 완료"

        // 초기 버튼 상태 설정
        checkAndUpdateDoneButtonState()
    }

    // 시간 유효성 검사 및 버튼 상태 업데이트 함수
    private fun checkAndUpdateDoneButtonState() {
        if (_binding == null) return

        val selectedHour = binding.hourPicker.value
        val selectedMinute = binding.minutePicker.value * 5
        var isValid = true

        if (targetTimeView == "time2") {
            val currentStartTime = sharedViewModel.getCurrentStartTime()
            if (currentStartTime != null) {
                val startTimeInMinutes = currentStartTime.first * 60 + currentStartTime.second
                val selectedEndTimeInMinutes = selectedHour * 60 + selectedMinute
                isValid = selectedEndTimeInMinutes > startTimeInMinutes
            }
        }

        Log.d("TimePicker", "checkAndUpdateDoneButtonState: target=$targetTimeView, selected=$selectedHour:$selectedMinute, isValid=$isValid")
        updateDoneButtonState(isValid)
    }

    // 버튼의 시각적 상태 및 활성화 상태 업데이트 함수 (colors.xml 리소스 사용)
    private fun updateDoneButtonState(isValid: Boolean) {
        // binding이 null이면 아무 작업도 하지 않음
        if (_binding == null) {
            Log.w("TimePicker", "updateDoneButtonState called when binding is null.")
            return
        }

        // 버튼의 클릭 가능 여부 설정
        binding.doneBtn.isEnabled = isValid
        binding.doneBtn.isClickable = isValid

        // ContextCompat.getColor를 사용하기 위해 context 가져오기
        val context = requireContext()

        if (isValid) {
            // 활성 상태 (colors.xml 리소스 사용)
            binding.btnBg.setColorFilter(ContextCompat.getColor(context, R.color.mainColor)) // 활성 배경색 (mainColor)
            binding.btnText.setTextColor(ContextCompat.getColor(context, R.color.white)) // 활성 텍스트색 (white)
        } else {
            // 비활성 상태 (colors.xml 리소스 사용)
            binding.btnBg.setColorFilter(ContextCompat.getColor(context, R.color.btn_inactive)) // 비활성 배경색 (btn_inactive)
            binding.btnText.setTextColor(ContextCompat.getColor(context, R.color.black)) // 비활성 텍스트색 (black)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("TimePickerBottomSheet", "onDestroyView 호출됨")
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TimePickerBottomSheet", "onDestroy 호출됨")
    }
}