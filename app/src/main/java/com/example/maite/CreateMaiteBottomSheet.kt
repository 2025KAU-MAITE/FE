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
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.maite.databinding.BottomSheetCreateMaiteBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.maite.model.CreateRoomRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CreateMaiteBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreateMaiteBinding? = null
    private val binding get() = _binding!!

    private var isDoneButtonEnabled = false

    // 선택된 사용자 정보 저장
    private val selectedUserIds = ArrayList<Long>()
    private val selectedUserNames = ArrayList<String>()
    private val selectedUserProfileUrls = ArrayList<String>()
    private val selectedUserEmails = ArrayList<String>()

    // API 서비스 인스턴스
    private lateinit var apiService: MaiteApiService

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
        // API 서비스 초기화
        apiService = MaiteRetrofitClient.getInstance(requireContext())
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
                createRoom()
            }
        }

        // 초기 UI 업데이트 (선택된 사용자가 없는 상태)
        updateInvitedUsersUI(0, emptyList(), emptyList())
    }

    // 새로운 함수: 룸 생성 API 호출
    private fun createRoom() {
        // UI에서 데이터 가져오기
        val name = binding.titleEditText.text.toString().trim()
        val description = binding.introEditText.text.toString().trim()

        // 로딩 표시 - 버튼 비활성화
        showLoading(true)

        // 선택된 이메일 로그 출력
        Log.d(TAG, "Creating room with name: $name, description: $description")
        Log.d(TAG, "Inviting emails: $selectedUserEmails")

        // API 요청 생성
        val request = CreateRoomRequest(
            name = name,
            description = description,
            inviteEmails = selectedUserEmails
        )

        // API 호출
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.createRoom(request)
                }

                // 메인 스레드에서 응답 처리
                if (response.isSuccessful) {
                    Log.d(TAG, "Room created successfully: HTTP ${response.code()}")

                    // 방 생성 완료 결과를 ListFragment에 전달
                    setFragmentResult(ROOM_CREATED_REQUEST_KEY, bundleOf(ROOM_CREATED_RESULT_KEY to true))

                    Toast.makeText(requireContext(), "MAITE 생성 완료", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Failed to create room: $errorBody")
                    Toast.makeText(requireContext(), "MAITE 생성 실패: $errorBody", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "API call failed", e)
                Toast.makeText(requireContext(), "오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    // 로딩 상태 표시/숨김
    private fun showLoading(isLoading: Boolean) {
        // 로딩 중에는 버튼 비활성화
        binding.doneBtn.isEnabled = !isLoading
        binding.doneBtn.isClickable = !isLoading

        if (isLoading) {
            binding.btnBg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.btn_inactive))
            binding.btnText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            updateDoneButtonAppearance(isDoneButtonEnabled)
        }
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
                Log.d("CreateMaiteBottomSheet", "Received selected emails: $emails")

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

        // 방 생성 완료 이벤트를 위한 상수
        const val ROOM_CREATED_REQUEST_KEY = "room_created_request"
        const val ROOM_CREATED_RESULT_KEY = "room_created"

        fun newInstance(): CreateMaiteBottomSheet {
            return CreateMaiteBottomSheet()
        }
    }
}