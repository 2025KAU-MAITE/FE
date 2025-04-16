package com.example.maite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.maite.databinding.BottomSheetSuggestBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SuggestBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetSuggestBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSuggestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 제목 입력 필드
        binding.title.setOnClickListener {
            // 여기에 제목 입력 다이얼로그 등을 표시
            Toast.makeText(context, "제목을 입력해주세요", Toast.LENGTH_SHORT).show()
        }

        // 날짜 선택 버튼
        binding.dateBtn.setOnClickListener {
            // 날짜 선택 다이얼로그 표시
            Toast.makeText(context, "날짜를 선택해주세요", Toast.LENGTH_SHORT).show()
        }

        // 시작 시간 선택
        binding.time1.setOnClickListener {
            // 시간 선택 다이얼로그 표시
            Toast.makeText(context, "시작 시간을 선택해주세요", Toast.LENGTH_SHORT).show()
        }

        // 종료 시간 선택
        binding.time2.setOnClickListener {
            // 시간 선택 다이얼로그 표시
            Toast.makeText(context, "종료 시간을 선택해주세요", Toast.LENGTH_SHORT).show()
        }

        // 완료 버튼
        binding.doneBtn.setOnClickListener {
            // 회의 제안 저장 및 전송 처리
            Toast.makeText(context, "회의 제안이 완료되었습니다", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): SuggestBottomSheet {
            return SuggestBottomSheet()
        }
    }
}