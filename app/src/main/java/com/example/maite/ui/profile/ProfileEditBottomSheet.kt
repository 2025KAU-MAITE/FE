package com.example.maite.ui.profile

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.maite.R
import com.example.maite.databinding.BottomsheetProfileEditBinding
import com.example.maite.model.UserInfo
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.UUID

class ProfileEditBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetProfileEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    private var selectedImageUri: Uri? = null
    private var isImageChanged = false
    private var originalImageUrl: String? = null
    
    // 갤러리에서 이미지 선택 결과를 처리하는 런처
    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    android.util.Log.d(TAG, "이미지 선택 성공: $uri")
                    
                    // 이미지 설정
                    selectedImageUri = uri
                    binding.ivProfileEdit.setImageURI(uri)
                    
                    // 이미지 변경 표시
                    isImageChanged = true
                    
                    // 하단 완료 버튼 활성화
                    binding.btnSaveProfile.isEnabled = true
                    
                    Toast.makeText(requireContext(), "이미지가 선택되었습니다. '완료' 버튼을 눌러 저장하세요.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "이미지 설정 중 오류 발생", e)
                    Toast.makeText(requireContext(), "이미지 설정 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                android.util.Log.e(TAG, "이미지 URI가 null입니다")
                Toast.makeText(requireContext(), "이미지를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getTheme(): Int {
        return R.style.AppBottomSheetDialogTheme
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.d(TAG, "onViewCreated 호출 - 바텐시트 생성")
        
        // 초기화 버튼 스타일 설정
        binding.tvResetProfile.setTextColor(Color.parseColor("#666666"))
        
        // 상태 초기화 (이미지 변경 여부 초기화)
        selectedImageUri = null
        isImageChanged = false
        
        // 하단 완료 버튼 초기화 - 비활성화 상태로 시작
        binding.btnSaveProfile.isEnabled = false
        binding.btnSaveProfile.visibility = View.VISIBLE
        
        setupListeners()
        loadProfile()
    }
    
    override fun onStart() {
        super.onStart()
        
        // 바텀시트 확장 설정
        val behavior = BottomSheetBehavior.from(requireView().parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true // 축소 상태 건너뛰기
        
        // 버튼 상태 로그
        android.util.Log.d(TAG, "버튼 상태 (onStart): visible=${binding.btnSaveProfile.visibility == View.VISIBLE}, enabled=${binding.btnSaveProfile.isEnabled}")
    }

    // 프로필 불러오기
    private fun loadProfile() {
        viewModel.userInfo.observe(viewLifecycleOwner) { userInfo: UserInfo? ->
            userInfo?.let {
                originalImageUrl = it.profileImageUrl
                
                if (it.profileImageUrl?.isNotEmpty() == true) {
                    Glide.with(requireContext())
                        .load(it.profileImageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.img_profile_default)
                        .error(R.drawable.img_profile_default)
                        .into(binding.ivProfileEdit)
                } else {
                    binding.ivProfileEdit.setImageResource(R.drawable.img_profile_default)
                }
            }
        }
    }

    private fun setupListeners() {
        // 사진 변경 버튼 클릭 리스너
        binding.ivChangePhoto.setOnClickListener {
            android.util.Log.d(TAG, "사진 변경 버튼 클릭")
            openGallery()
        }
        
        // 프로필 이미지 클릭 시에도 갤러리 열기
        binding.ivProfileEdit.setOnClickListener {
            android.util.Log.d(TAG, "프로필 이미지 클릭")
            openGallery()
        }

        // 초기화 버튼 클릭 리스너
        binding.tvResetProfile.setOnClickListener {
            android.util.Log.d(TAG, "초기화 버튼 클릭")
            resetProfileImage()
        }

        // 하단 완료 버튼 클릭 리스너
        binding.btnSaveProfile.setOnClickListener {
            android.util.Log.d(TAG, "하단 완료 버튼 클릭: isEnabled=${binding.btnSaveProfile.isEnabled}, isImageChanged=$isImageChanged")
            
            if (isImageChanged) {
                selectedImageUri?.let { uri ->
                    uploadProfileImage(uri)
                } ?: run {
                    // 이미지가 초기화된 경우
                    saveDefaultProfileImage()
                }
            } else {
                android.util.Log.w(TAG, "이미지가 변경되지 않았지만 완료 버튼이 클릭됨")
                Toast.makeText(requireContext(), "이미지를 변경한 후 저장해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        getContent.launch(intent)
    }
    
    private fun resetProfileImage() {
        // 기본 프로필 이미지로 변경
        binding.ivProfileEdit.setImageResource(R.drawable.img_profile_default)
        selectedImageUri = null
        isImageChanged = true
        
        // 하단 완료 버튼 활성화
        binding.btnSaveProfile.isEnabled = true
        
        Toast.makeText(requireContext(), "기본 이미지로 초기화됩니다. 저장하려면 '완료' 버튼을 눌러주세요.", Toast.LENGTH_SHORT).show()
    }
    
    private fun saveDefaultProfileImage() {
        // 서버에 기본 이미지로 저장하는 로직
        // 실제 기본 이미지 URL을 서버에 전송하거나, null을 전송하여 기본 이미지로 설정
        showLoading(true)
        
        viewModel.resetProfileImage().observe(viewLifecycleOwner) { result ->
            showLoading(false)
            
            if (result != null && result) {
                // 초기화 성공
                Toast.makeText(requireContext(), "프로필 이미지가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
                
                // 프로필 이미지 갱신 이벤트 전달
                (parentFragment as? ProfileImageUpdateListener)?.onProfileImageUpdated()
                
                dismiss()
            } else {
                // 초기화 실패
                Toast.makeText(requireContext(), "프로필 이미지 초기화에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadProfileImage(uri: Uri) {
        // 로딩 표시
        showLoading(true)
        
        // 실제 파일 이름 생성 (UUID로 고유한 파일명 생성)
        val fileName = "profile_${UUID.randomUUID()}.jpg"
        
        // 뷰모델을 통해 이미지 업로드 (서버 통신)
        viewModel.uploadProfileImage(uri, fileName).observe(viewLifecycleOwner) { result ->
            showLoading(false)
            
            if (result != null && result) {
                // 업로드 성공
                Toast.makeText(requireContext(), "프로필 이미지가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                
                // 프로필 이미지 갱신 이벤트 전달
                (parentFragment as? ProfileImageUpdateListener)?.onProfileImageUpdated()
                
                dismiss()
            } else {
                // 업로드 실패
                Toast.makeText(requireContext(), "이미지 업로드에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            // 로딩 중일 때
            binding.btnSaveProfile.isEnabled = false
            binding.btnSaveProfile.text = "저장 중..."
            
            binding.tvResetProfile.isEnabled = false
            binding.ivChangePhoto.isEnabled = false
        } else {
            // 로딩 완료 시
            binding.btnSaveProfile.text = "완료"
            
            // 이미지 변경 여부에 따라 버튼 상태 갱신
            binding.btnSaveProfile.isEnabled = isImageChanged
            
            binding.tvResetProfile.isEnabled = true
            binding.ivChangePhoto.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 프로필 이미지 업데이트 리스너 인터페이스
    interface ProfileImageUpdateListener {
        fun onProfileImageUpdated()
    }

    companion object {
        const val TAG = "ProfileEditBottomSheet"
        
        fun newInstance(): ProfileEditBottomSheet {
            return ProfileEditBottomSheet()
        }
    }
}