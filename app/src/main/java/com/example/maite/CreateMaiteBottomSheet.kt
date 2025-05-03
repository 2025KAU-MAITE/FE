package com.example.maite

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat // ContextCompat import 확인
import com.example.maite.databinding.BottomSheetCreateMaiteBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.maite.R // R import 확인

class CreateMaiteBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreateMaiteBinding? = null
    private val binding get() = _binding!!

    private var isDoneButtonEnabled = false

    // TextWatcher 인스턴스를 멤버 변수로 저장 (onDestroyView에서 제거하기 위함)
    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            checkInputsAndUpdateButtonState()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCreateMaiteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // EditText에 TextWatcher 연결
        binding.titleEditText.addTextChangedListener(textWatcher)
        binding.introEditText.addTextChangedListener(textWatcher)

        // 초기 버튼 상태 설정
        checkInputsAndUpdateButtonState()

        // addBtn 클릭 리스너
        binding.addBtn.setOnClickListener {
            val inviteBottomSheet = InviteBottomSheet.newInstance()
            inviteBottomSheet.show(parentFragmentManager, "InviteBottomSheetTag")
        }

        // doneBtn 클릭 리스너
        binding.doneBtn.setOnClickListener {
            if (isDoneButtonEnabled) {
                dismiss()
                Toast.makeText(requireContext(), "MAITE 생성 완료", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 입력 상태 확인 및 버튼 상태 업데이트 함수
    private fun checkInputsAndUpdateButtonState() {
        val title = binding.titleEditText.text.toString().trim()
        val intro = binding.introEditText.text.toString().trim()

        isDoneButtonEnabled = title.isNotEmpty() && intro.isNotEmpty()

        updateDoneButtonAppearance(isDoneButtonEnabled)
    }

    private fun updateDoneButtonAppearance(isEnabled: Boolean) {
        val context = requireContext()
        if (isEnabled) {
            binding.doneBtn.isClickable = true // 이거 없어도 잘 작동은 함
            binding.btnBg.setColorFilter(ContextCompat.getColor(context, R.color.mainColor))
            binding.btnText.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            binding.doneBtn.isClickable = false
            binding.btnBg.setColorFilter(ContextCompat.getColor(context, R.color.btn_inactive))
            binding.btnText.setTextColor(ContextCompat.getColor(context, R.color.black))
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        // TextWatcher 제거
        binding.titleEditText.removeTextChangedListener(textWatcher)
        binding.introEditText.removeTextChangedListener(textWatcher)
        _binding = null
    }

    companion object {
        const val TAG = "CreateMaiteBottomSheet"
        fun newInstance(): CreateMaiteBottomSheet {
            return CreateMaiteBottomSheet()
        }
    }
}