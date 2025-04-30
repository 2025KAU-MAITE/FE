package com.example.maite

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels // activityViewModels 임포트 확인
import com.example.maite.databinding.BottomSheetTimePickerBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TimePickerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTimePickerBinding? = null
    private val binding get() = _binding!!

    // ViewModel 공유 (Activity 스코프)
    private val sharedViewModel: TimeSelectionViewModel by activityViewModels()

    // 'time1' (시작) 또는 'time2' (종료) 구분
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
            Log.d("TimePicker", "newInstance 호출: target=$targetTimeView, initial=$initialHour:$initialMinute")
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
        Log.d("TimePicker", "onViewCreated 호출됨")

        targetTimeView = arguments?.getString(KEY_TARGET_TIME_VIEW) ?: "time1"
        val initialHour = arguments?.getInt(KEY_INITIAL_HOUR, 0) ?: 0
        // 분 초기값은 5의 배수로 보정
        val initialMinute = (arguments?.getInt(KEY_INITIAL_MINUTE, 0) ?: 0) / 5 * 5

        // --- 중요: 날짜 선택 여부 확인 ---
        if (sharedViewModel.getCurrentDate() == null) {
            Log.e("TimePicker", "날짜가 선택되지 않아 TimePicker를 초기화할 수 없습니다.")
            Toast.makeText(context, "날짜를 먼저 선택해야 시간을 지정할 수 있습니다.", Toast.LENGTH_LONG).show()
            // UI 비활성화 및 시각적 피드백
            binding.hourPicker.isEnabled = false
            binding.minutePicker.isEnabled = false
            binding.doneBtn.isEnabled = false
            updateDoneButtonStateVisuals(false) // 시각적 비활성화
            // 필요하다면 dismiss() 호출하여 바로 닫기
            // dismiss()
            return // 이후 로직 실행 중단
        }
        // --- 종료 시간 선택 시 시작 시간 선택 여부 확인 ---
        if (targetTimeView == "time2" && sharedViewModel.getCurrentStartTime() == null) {
            Log.e("TimePicker", "시작 시간이 선택되지 않아 종료 시간을 지정할 수 없습니다.")
            Toast.makeText(context, "시작 시간을 먼저 선택해주세요.", Toast.LENGTH_LONG).show()
            binding.hourPicker.isEnabled = false
            binding.minutePicker.isEnabled = false
            binding.doneBtn.isEnabled = false
            updateDoneButtonStateVisuals(false)
            return
        }


        // --- NumberPicker 설정 ---
        binding.hourPicker.minValue = 0
        binding.hourPicker.maxValue = 23
        binding.hourPicker.setFormatter { String.format("%02d", it) }
        binding.hourPicker.value = initialHour.coerceIn(0, 23) // 범위 보정

        binding.minutePicker.minValue = 0
        binding.minutePicker.maxValue = 11 // 0~11 (인덱스)
        val minuteValues = Array(12) { String.format("%02d", it * 5) } // 00, 05, ..., 55
        binding.minutePicker.displayedValues = minuteValues
        val initialMinuteIndex = (initialMinute / 5).coerceIn(0, 11) // 인덱스 계산 및 보정
        binding.minutePicker.value = initialMinuteIndex

        // --- NumberPicker 값 변경 리스너 ---
        val valueChangeListener = NumberPicker.OnValueChangeListener { _, _, _ ->
            Log.d("TimePicker", "Picker 값 변경됨. 유효성 검사 시작.")
            checkAndUpdateDoneButtonState() // 값 변경 시마다 유효성 검사 및 버튼 상태 업데이트
        }
        binding.hourPicker.setOnValueChangedListener(valueChangeListener)
        binding.minutePicker.setOnValueChangedListener(valueChangeListener)

        // --- 완료 버튼 클릭 리스너 ---
        binding.doneBtn.setOnClickListener {
            // 버튼 비활성화 시 클릭 무시
            if (!binding.doneBtn.isEnabled) {
                Log.w("TimePicker", "완료 버튼 비활성화 상태 클릭됨.")
                Toast.makeText(context, "선택할 수 없는 시간입니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedHour = binding.hourPicker.value
            val selectedMinute = binding.minutePicker.value * 5 // 실제 분 값

            Log.d("TimePicker", "완료 버튼 클릭됨: target=$targetTimeView, 선택=$selectedHour:$selectedMinute")

            // ViewModel 업데이트
            if (targetTimeView == "time1") {
                sharedViewModel.updateStartTime(selectedHour, selectedMinute)
                // 시작 시간 변경 후, 기존 종료 시간이 유효하지 않게 되면 초기화 (선택적)
                val currentEndTime = sharedViewModel.getCurrentEndTime()
                if (currentEndTime != null) {
                    if (!sharedViewModel.isValidEndTime(currentEndTime.first, currentEndTime.second)) {
                        Log.d("TimePicker", "시작 시간 변경으로 인해 기존 종료 시간 초기화")
                        sharedViewModel.updateEndTime(0, 0) // 또는 null
                        // 사용자에게 알림 (선택적)
                        // Toast.makeText(context, "종료 시간이 시작 시간보다 빠르거나 같아 초기화됩니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else { // targetTimeView == "time2"
                sharedViewModel.updateEndTime(selectedHour, selectedMinute)
            }

            Log.d("TimePicker", "ViewModel 업데이트 완료 후 dismiss 호출")
            dismiss() // 바텀 시트 닫기
        }
        binding.btnText.text = "선택 완료" // 버튼 텍스트 설정

        // 초기 로드 시 유효성 검사 및 버튼 상태 설정
        Log.d("TimePicker", "초기 유효성 검사 시작.")
        checkAndUpdateDoneButtonState()
    }

    // 시간 유효성 검사 및 완료 버튼 상태 업데이트 함수
    private fun checkAndUpdateDoneButtonState() {
        // 바인딩 유효성 체크
        if (_binding == null) {
            Log.w("TimePicker", "checkAndUpdateDoneButtonState 호출 시 바인딩이 null입니다.")
            return
        }
        // 날짜 선택 여부 재확인 (필수)
        if (sharedViewModel.getCurrentDate() == null) {
            Log.w("TimePicker", "날짜가 선택되지 않아 유효성 검사 불가.")
            updateDoneButtonStateVisuals(false)
            return
        }
        // 종료 시간 선택 시 시작 시간 선택 여부 재확인 (필수)
        if (targetTimeView == "time2" && sharedViewModel.getCurrentStartTime() == null) {
            Log.w("TimePicker", "시작 시간이 선택되지 않아 종료 시간 유효성 검사 불가.")
            updateDoneButtonStateVisuals(false)
            return
        }


        val selectedHour = binding.hourPicker.value
        val selectedMinute = binding.minutePicker.value * 5
        var isValid = false // 기본값 false

        try {
            // ViewModel의 유효성 검사 함수 호출
            isValid = if (targetTimeView == "time1") {
                sharedViewModel.isValidStartTime(selectedHour, selectedMinute)
            } else {
                sharedViewModel.isValidEndTime(selectedHour, selectedMinute)
            }
        } catch (e: Exception) {
            // ViewModel 접근 또는 로직 실행 중 예외 발생 가능성 대비
            Log.e("TimePicker", "유효성 검사 중 예외 발생", e)
            isValid = false // 오류 시 비활성화
        }

        Log.d("TimePicker", "유효성 검사 결과: $isValid ($selectedHour:$selectedMinute)")
        // 시각적/활성화 상태 업데이트
        updateDoneButtonStateVisuals(isValid)
    }

    // 완료 버튼의 시각적 상태 및 활성화 상태 업데이트 함수
    private fun updateDoneButtonStateVisuals(isValid: Boolean) {
        if (_binding == null) {
            Log.w("TimePicker", "updateDoneButtonStateVisuals 호출 시 바인딩이 null입니다.")
            return
        }

        binding.doneBtn.isEnabled = isValid
        binding.doneBtn.isClickable = isValid

        // Context 가져오기 (null 체크 포함)
        val context = context ?: run {
            Log.e("TimePicker", "Context가 null이라 버튼 색상 업데이트 불가.")
            return
        }

        // colors.xml 리소스 사용
        val bgColor = ContextCompat.getColor(context, if (isValid) R.color.mainColor else R.color.btn_inactive)
        val textColor = ContextCompat.getColor(context, if (isValid) R.color.white else R.color.black) // 비활성 시 텍스트 색상 확인

        binding.btnBg.setColorFilter(bgColor)
        binding.btnText.setTextColor(textColor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("TimePicker", "onDestroyView 호출됨")
        _binding = null // 메모리 누수 방지
    }

    // onDestroy는 Fragment 생명주기에서 호출되지만, 특별한 정리 작업 없으면 생략 가능
    // override fun onDestroy() {
    //     super.onDestroy()
    //     Log.d("TimePicker", "onDestroy 호출됨")
    // }
}