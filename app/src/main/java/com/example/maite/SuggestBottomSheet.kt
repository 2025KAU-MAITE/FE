package com.example.maite

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.maite.databinding.BottomSheetSuggestBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SuggestBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSuggestBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: TimeSelectionViewModel by viewModels()

    companion object {
        fun newInstance(): SuggestBottomSheet {
            return SuggestBottomSheet()
        }
    }

    // onCreate 수정: super.onCreate 호출 추가
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // super 호출 추가
        Log.d("SuggestBottomSheet", "onCreate 호출됨")
    }

    // onCreateView 수정: 정확한 시그니처 및 내용으로 변경
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, // Nullable
        savedInstanceState: Bundle?
    ): View? { // Nullable 반환 타입
        Log.d("SuggestBottomSheet", "onCreateView 호출됨")
        // inflater, container 파라미터 사용
        _binding = BottomSheetSuggestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("SuggestBottomSheet", "onViewCreated 호출됨")

        setupObservers() // LiveData 관찰 설정

        // --- 기존 버튼 리스너들 (동일) ---
        binding.title.setOnClickListener {
            Toast.makeText(context, "제목 입력 로직 구현 필요", Toast.LENGTH_SHORT).show()
        }
        binding.dateBtn.setOnClickListener {
            Toast.makeText(context, "날짜 선택 로직 구현 필요", Toast.LENGTH_SHORT).show()
        }
        binding.time1.setOnClickListener {
            val currentTime = parseTimeFromTextView(binding.time1Text)
            val timePicker = TimePickerBottomSheet.newInstance(
                targetTimeView = "time1",
                initialHour = currentTime.first,
                initialMinute = currentTime.second
            )
            Log.d("SuggestBottomSheet", "TimePickerBottomSheet 표시 (childFragmentManager) for time1")
            timePicker.show(childFragmentManager, "timePicker1")
        }
        binding.time2.setOnClickListener {
            val currentTime = parseTimeFromTextView(binding.time2Text)
            val timePicker = TimePickerBottomSheet.newInstance(
                targetTimeView = "time2",
                initialHour = currentTime.first,
                initialMinute = currentTime.second
            )
            Log.d("SuggestBottomSheet", "TimePickerBottomSheet 표시 (childFragmentManager) for time2")
            timePicker.show(childFragmentManager, "timePicker2")
        }
        binding.doneBtn.setOnClickListener {
            val startTime = sharedViewModel.getCurrentStartTime()
            val endTime = sharedViewModel.getCurrentEndTime()
            if (startTime != null && endTime != null) {
                val startTimeInMinutes = startTime.first * 60 + startTime.second
                val endTimeInMinutes = endTime.first * 60 + endTime.second
                if (endTimeInMinutes <= startTimeInMinutes) {
                    Toast.makeText(context, "종료 시간은 시작 시간보다 늦어야 합니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else if (startTime == null || endTime == null) {
                Toast.makeText(context, "시작 시간과 종료 시간을 모두 설정해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(context, "회의 제안이 완료되었습니다", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    // LiveData 관찰 설정 함수 (이전과 동일)
    private fun setupObservers() {
        Log.d("SuggestBottomSheet", "setupObservers 호출됨 - LiveData 관찰 시작")

        sharedViewModel.startTime.observe(viewLifecycleOwner, Observer { startTimePair ->
            if (startTimePair != null) {
                Log.d("SuggestBottomSheet", "시작 시간 LiveData 변경 감지: ${startTimePair.first}:${startTimePair.second}")
                if (_binding != null) {
                    binding.time1Text.text = String.format("%02d : %02d", startTimePair.first, startTimePair.second)
                } else {
                    Log.e("SuggestBottomSheet", "시작 시간 콜백 수신 시 binding이 null입니다.")
                }
            } else {
                if (_binding != null) binding.time1Text.text = "00 : 00"
                Log.d("SuggestBottomSheet", "시작 시간 LiveData가 null입니다.")
            }
        })

        sharedViewModel.endTime.observe(viewLifecycleOwner, Observer { endTimePair ->
            if (endTimePair != null) {
                Log.d("SuggestBottomSheet", "종료 시간 LiveData 변경 감지: ${endTimePair.first}:${endTimePair.second}")
                if (_binding != null) {
                    binding.time2Text.text = String.format("%02d : %02d", endTimePair.first, endTimePair.second)
                } else {
                    Log.e("SuggestBottomSheet", "종료 시간 콜백 수신 시 binding이 null입니다.")
                }
            } else {
                if (_binding != null) binding.time2Text.text = "00 : 00"
                Log.d("SuggestBottomSheet", "종료 시간 LiveData가 null입니다.")
            }
        })
    }

    // --- 기존 parseTimeFromTextView, onDestroyView, onDestroy (동일) ---
    private fun parseTimeFromTextView(textView: TextView): Pair<Int, Int> {
        val timeString = textView.text.toString()
        var hour = 0
        var minute = 0
        try {
            if (timeString.contains(":")) {
                val parts = timeString.split(":")
                if (parts.size == 2) {
                    hour = parts[0].trim().toIntOrNull() ?: 0
                    minute = parts[1].trim().toIntOrNull() ?: 0
                }
            }
        } catch (e: Exception) {
            Log.e("SuggestBottomSheet", "시간 파싱 오류: $timeString", e)
            hour = 0
            minute = 0
        }
        if (hour !in 0..23) hour = 0
        if (minute !in 0..59) minute = 0
        return Pair(hour, minute)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("SuggestBottomSheet", "onDestroyView 호출됨")
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("SuggestBottomSheet", "onDestroy 호출됨")
    }
}