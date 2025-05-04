package com.example.maite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.maite.databinding.BottomSheetEditMeetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EditMeetBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEditMeetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEditMeetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            binding.titleEditText.setText(it.getString(ARG_CURRENT_TITLE))
            binding.date.text = it.getString(ARG_CURRENT_DATE)
            // 시간 데이터 파싱 및 설정 (예: "08:00 ~ 10:00" -> "08 : 00", "10 : 00")
            val timeString = it.getString(ARG_CURRENT_TIME)
            if (timeString != null && timeString.contains("~")) {
                val times = timeString.split("~").map { t -> t.trim() }
                if (times.size == 2) {
                    binding.time1Text.text = times[0].replace(":", " : ") // UI 형식에 맞게 변경
                    binding.time2Text.text = times[1].replace(":", " : ") // UI 형식에 맞게 변경
                } else {
                    binding.time1Text.text = "00 : 00" // 기본값 또는 오류 처리
                    binding.time2Text.text = "00 : 00"
                }
            } else {
                binding.time1Text.text = "00 : 00" // 기본값 또는 오류 처리
                binding.time2Text.text = "00 : 00"
            }
            binding.place.text = it.getString(ARG_CURRENT_PLACE)
        }
        // --- UI 필드 채우기 끝 ---


        // --- 리스너 설정 ---
        binding.dateBtn.setOnClickListener {
            // TODO: 날짜 선택 로직 구현
        }

        binding.time1.setOnClickListener {
            // TODO: 시작 시간 선택 로직 구현
        }

        binding.time2.setOnClickListener {
            // TODO: 종료 시간 선택 로직 구현
        }

        binding.placeCardView.setOnClickListener {
            // TODO: 장소 선택 로직 구현
        }

        binding.doneBtn.setOnClickListener {
            // TODO: 수정된 정보 저장 처리
            // 1. 현재 UI 필드의 값 가져오기 (binding.titleEditText.text 등)
            // 2. 데이터 유효성 검사
            // 3. (필요하다면) ViewModel 또는 FragmentResult API를 통해 MeetDetailFragment로 결과 전달
            dismiss() // 바텀 시트 닫기
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "EditMeetBottomSheet"
        // newInstance 함수는 이제 필수는 아니지만, 필요하다면 arguments 설정 로직을 포함하도록 수정할 수 있습니다.
    }
}