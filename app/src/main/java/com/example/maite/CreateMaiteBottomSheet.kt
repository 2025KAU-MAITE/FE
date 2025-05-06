package com.example.maite

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResultListener
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.maite.databinding.BottomSheetCreateMaiteBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.maite.R

class CreateMaiteBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreateMaiteBinding? = null
    private val binding get() = _binding!!

    private var isDoneButtonEnabled = false

    // 선택된 사용자 정보 저장
    private val selectedUserIds = ArrayList<Long>()
    private val selectedUserNames = ArrayList<String>()
    private val selectedUserProfileUrls = ArrayList<String>()
    private val selectedUserEmails = ArrayList<String>()

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            checkInputsAndUpdateButtonState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFragmentResultListener()
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

        binding.titleEditText.addTextChangedListener(textWatcher)
        binding.introEditText.addTextChangedListener(textWatcher)

        checkInputsAndUpdateButtonState()

        binding.addBtn.setOnClickListener {
            // 현재 선택된 사용자 ID 목록 로그로 확인
            Log.d("CreateMaiteBottomSheet", "Selected IDs to pass: $selectedUserIds")

            // 이미 선택된 사용자 ID 목록을 InviteBottomSheet에 전달
            val inviteBottomSheet = InviteBottomSheet.newInstance(selectedUserIds)
            inviteBottomSheet.show(parentFragmentManager, InviteBottomSheet::class.java.simpleName)
        }

        binding.doneBtn.setOnClickListener {
            if (isDoneButtonEnabled) {
                val title = binding.titleEditText.text.toString()
                val intro = binding.introEditText.text.toString()

                // 여기에서 선택된 이메일 정보를 활용할 수 있음
                // 예: API 호출 시 초대할 사용자 이메일 목록 전달

                dismiss()
                Toast.makeText(requireContext(), "MAITE 생성 완료", Toast.LENGTH_SHORT).show()
            }
        }

        // 초기 UI 업데이트 (선택된 사용자가 없는 상태)
        updateInvitedUsersUI(0, emptyList(), emptyList())
    }

    private fun setupFragmentResultListener() {
        setFragmentResultListener(InviteBottomSheet.REQUEST_KEY) { requestKey, bundle ->
            if (requestKey == InviteBottomSheet.REQUEST_KEY) {
                val selectedCount = bundle.getInt(InviteBottomSheet.KEY_SELECTED_COUNT, 0)

                // 선택된 사용자 정보 가져오기
                selectedUserIds.clear()
                selectedUserNames.clear()
                selectedUserProfileUrls.clear()
                selectedUserEmails.clear()

                // ArrayList로 가져오기 (변경된 부분)
                val ids = bundle.getSerializable(InviteBottomSheet.KEY_SELECTED_IDS) as? ArrayList<*>
                val names = bundle.getStringArrayList(InviteBottomSheet.KEY_SELECTED_NAMES)
                val profileUrls = bundle.getStringArrayList(InviteBottomSheet.KEY_SELECTED_PROFILE_URLS)
                val emails = bundle.getStringArrayList(InviteBottomSheet.KEY_SELECTED_EMAILS)

                // ID 목록 처리
                if (ids != null) {
                    for (id in ids) {
                        if (id is Long) {
                            selectedUserIds.add(id)
                        } else if (id is Int) {
                            // Int인 경우 Long으로 변환
                            selectedUserIds.add(id.toLong())
                        }
                    }
                }

                // 디버그 로그 추가
                Log.d("CreateMaiteBottomSheet", "Received selected IDs: $selectedUserIds")

                if (names != null) {
                    selectedUserNames.addAll(names)
                }

                if (profileUrls != null) {
                    selectedUserProfileUrls.addAll(profileUrls)
                }

                if (emails != null) {
                    selectedUserEmails.addAll(emails)
                }

                updateInvitedUsersUI(selectedCount, selectedUserProfileUrls, selectedUserEmails)
            }
        }
    }

    private fun updateInvitedUsersUI(count: Int, profileUrls: List<String>, emails: List<String>) {
        val childrenToRemove = mutableListOf<View>()
        for (i in 0 until binding.invitedUsersLayout.childCount) {
            val child = binding.invitedUsersLayout.getChildAt(i)
            if (child is ImageView && child.id != R.id.addBtn) {
                childrenToRemove.add(child)
            }
        }
        childrenToRemove.forEach { binding.invitedUsersLayout.removeView(it) }

        if (count > 0) {
            val imageSize = resources.getDimensionPixelSize(R.dimen.invited_profile_img_size)
            val desiredMarginDp = 8
            val imageMarginEnd = (desiredMarginDp * resources.displayMetrics.density).toInt()

            val addBtn = binding.invitedUsersLayout.findViewById<ImageView>(R.id.addBtn)
            val addBtnIndex = if (addBtn != null) binding.invitedUsersLayout.indexOfChild(addBtn) else 0

            for (i in 0 until count) {
                val imageView = ImageView(requireContext())
                val layoutParams = LinearLayout.LayoutParams(imageSize, imageSize)

                layoutParams.marginEnd = imageMarginEnd

                imageView.layoutParams = layoutParams

                // Glide를 사용하여 프로필 이미지 로드
                val profileUrl = if (i < profileUrls.size) profileUrls[i] else ""
                val email = if (i < emails.size) emails[i] else ""

                if (profileUrl.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(profileUrl)
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(R.drawable.img_profile_default)
                        .error(R.drawable.img_profile_default)
                        .into(imageView)
                } else {
                    imageView.setImageResource(R.drawable.img_profile_default)
                }

                // 이메일 정보를 이미지 태그에 저장 (필요시 나중에 접근 가능)
                imageView.tag = email

                imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                binding.invitedUsersLayout.addView(imageView, addBtnIndex + i)
            }
        }
        binding.invitedUsersScrollView.post {
            binding.invitedUsersScrollView.fullScroll(View.FOCUS_RIGHT)
        }
    }

    private fun checkInputsAndUpdateButtonState() {
        val title = binding.titleEditText.text.toString().trim()
        val intro = binding.introEditText.text.toString().trim()
        isDoneButtonEnabled = title.isNotEmpty() && intro.isNotEmpty()
        updateDoneButtonAppearance(isDoneButtonEnabled)
    }

    private fun updateDoneButtonAppearance(isEnabled: Boolean) {
        val context = requireContext()
        binding.doneBtn.isClickable = isEnabled
        if (isEnabled) {
            binding.btnBg.setColorFilter(ContextCompat.getColor(context, R.color.mainColor))
            binding.btnText.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            binding.btnBg.setColorFilter(ContextCompat.getColor(context, R.color.btn_inactive))
            binding.btnText.setTextColor(ContextCompat.getColor(context, R.color.white))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
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