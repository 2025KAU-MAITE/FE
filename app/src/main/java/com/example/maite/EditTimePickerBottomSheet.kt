package com.example.maite.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.example.maite.R
import com.example.maite.databinding.BottomSheetTimePickerBinding  // 여기 수정
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EditTimePickerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTimePickerBinding? = null  // 여기 수정
    private val binding get() = _binding!!

    // 시간표 수정용 ViewModel 사용
    private val viewModel: EditTimeSelectionViewModel by activityViewModels()

    // 'time1' (시작) 또는 'time2' (종료) 구분
    private var targetTimeView: String = "time1"

    companion object {
        const val KEY_TARGET_TIME_VIEW = "targetTimeView"
        const val KEY_INITIAL_HOUR = "initialHour"
        const val KEY_INITIAL_MINUTE = "initialMinute"

        fun newInstance(targetTimeView: String, initialHour: Int, initialMinute: Int): EditTimePickerBottomSheet {
            val fragment = EditTimePickerBottomSheet()
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
    ): View {
        _binding = BottomSheetTimePickerBinding.inflate(inflater, container, false)  // 여기 수정
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        targetTimeView = arguments?.getString(KEY_TARGET_TIME_VIEW) ?: "time1"
        val initialHour = arguments?.getInt(KEY_INITIAL_HOUR, 0) ?: 0
        // 분 초기값은 30분 단위로 보정 (0 또는 30)
        val initialMinute = if (arguments?.getInt(KEY_INITIAL_MINUTE, 0) ?: 0 >= 30) 30 else 0

        // --- NumberPicker 설정 ---
        binding.hourPicker.minValue = 0
        binding.hourPicker.maxValue = 23
        binding.hourPicker.setFormatter { String.format("%02d", it) }
        binding.hourPicker.value = initialHour.coerceIn(0, 23)

        // 30분 단위 설정 (0, 30)
        binding.minutePicker.minValue = 0
        binding.minutePicker.maxValue = 1 // 0~1 (30분 단위 인덱스)
        val minuteValues = Array(2) { if (it == 0) "00" else "30" } // 00, 30
        binding.minutePicker.displayedValues = minuteValues
        binding.minutePicker.value = if (initialMinute >= 30) 1 else 0

        // --- 완료 버튼 항상 활성화 ---
        binding.doneBtn.isEnabled = true
        binding.btnBg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.mainColor))
        binding.btnText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        binding.btnText.text = "선택 완료"

        // --- 완료 버튼 클릭 리스너 ---
        binding.doneBtn.setOnClickListener {
            val selectedHour = binding.hourPicker.value
            val selectedMinute = binding.minutePicker.value * 30 // 0 또는 30

            // 종료 시간이 시작 시간보다 이전이면 경고 표시 (선택은 가능)
            if (targetTimeView == "time2") {
                val startTime = viewModel.getCurrentStartTime()
                if (startTime != null) {
                    val startTotalMinutes = startTime.first * 60 + startTime.second
                    val endTotalMinutes = selectedHour * 60 + selectedMinute

                    if (endTotalMinutes <= startTotalMinutes) {
                        Toast.makeText(context, "종료 시간이 시작 시간보다 빠릅니다.", Toast.LENGTH_SHORT).show()
                        // 경고만 표시, 선택은 계속 진행
                    }
                }
            }

            // ViewModel 업데이트
            if (targetTimeView == "time1") {
                viewModel.updateStartTime(selectedHour, selectedMinute)
            } else {
                viewModel.updateEndTime(selectedHour, selectedMinute)
            }

            dismiss() // 바텀 시트 닫기
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}