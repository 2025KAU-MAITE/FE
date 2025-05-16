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
import com.example.maite.PreferencesUtil
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
                    
                    // 이미지 설정 (바텀시트 내에서만 표시)
                    selectedImageUri = uri
                    
                    // Glide를 사용하여 이미지 설정 (Uri 바로 사용 대신)
                    Glide.with(requireContext())
                        .load(uri)
                        .centerCrop()
                        .skipMemoryCache(true) // 메모리 캐시 사용 안 함
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE) // 디스크 캐시도 사용 안 함
                        .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis().toString()))
                        .placeholder(R.drawable.img_profile_default)
                        .error(R.drawable.img_profile_default)
                        .into(binding.ivProfileEdit)
                    
                    // 이미지 변경 표시
                    isImageChanged = true
                    
                    // 하단 완료 버튼 활성화
                    binding.btnSaveProfile.isEnabled = true
                    
                    // 임시 URI 저장 (완료 버튼을 눌러야 실제 적용)
                    val preferencesUtil = PreferencesUtil(requireContext())
                    preferencesUtil.setString("user_profile_image_uri_temp", uri.toString())
                    android.util.Log.d(TAG, "임시 이미지 URI 저장함: $uri")
                    
                    // 임시 URI 상태를 ProfileViewModel에도 공유
                    viewModel.setTempProfileImageUri(uri.toString())
                    
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
        
        android.util.Log.d(TAG, "onViewCreated 호출 - 바텀시트 생성")
        
        // 초기화 버튼 스타일 설정
        binding.tvResetProfile.setTextColor(Color.parseColor("#666666"))
        
        // 상태 초기화 (이미지 변경 여부 초기화)
        selectedImageUri = null
        isImageChanged = false
        
        // 하단 완료 버튼 초기화 - 비활성화 상태로 시작
        binding.btnSaveProfile.isEnabled = false
        binding.btnSaveProfile.visibility = View.VISIBLE
        
        // ProfileFragment로부터 현재 이미지 정보 가져오기
        val preferencesUtil = PreferencesUtil(requireContext())
        val cachedImageUrl = preferencesUtil.getString("user_profile_image_url")
        val localImageUri = preferencesUtil.getString("user_profile_image_uri")
        
        // 전달받은 이미지 URI 파라미터 확인
        val argumentImageUri = arguments?.getString(ARG_CURRENT_IMAGE_URI)
        android.util.Log.d(TAG, "전달받은 이미지 URI: $argumentImageUri")
        android.util.Log.d(TAG, "로컬 이미지 URI: $localImageUri")
        android.util.Log.d(TAG, "캐시된 이미지 URL: $cachedImageUrl")
        
        // 이미지 소스 선택 (우선순위: 전달받은 URI > 로컬 저장 URI > 캐시된 URL)
        val imageSource = when {
            !argumentImageUri.isNullOrEmpty() -> argumentImageUri
            !localImageUri.isNullOrEmpty() -> localImageUri
            !cachedImageUrl.isNullOrEmpty() -> cachedImageUrl
            else -> null
        }
        
        android.util.Log.d(TAG, "선택된 이미지 소스: $imageSource")
        
        // 선택된 이미지 소스가 있으면 바텀시트 이미지로 로드
        if (imageSource != null) {
            preferencesUtil.setString("active_bottomsheet_image", imageSource)
            
            try {
                // URI인지 URL인지 확인
                val isUri = imageSource.startsWith("content:") || imageSource.startsWith("file:")
                
                if (isUri) {
                    // URI로 이미지 로드
                    val uri = Uri.parse(imageSource)
                    android.util.Log.d(TAG, "URI로 이미지 즉시 로드: $uri")
                    
                    Glide.with(requireContext())
                        .load(uri)
                        .centerCrop()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                        .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis().toString()))
                        .placeholder(R.drawable.img_profile_default)
                        .error(R.drawable.img_profile_default)
                        .into(binding.ivProfileEdit)
                } else {
                    // URL로 이미지 로드
                    android.util.Log.d(TAG, "URL로 이미지 즉시 로드: $imageSource")
                    
                    Glide.with(requireContext())
                        .load(imageSource)
                        .centerCrop()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                        .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis().toString()))
                        .placeholder(R.drawable.img_profile_default)
                        .error(R.drawable.img_profile_default)
                        .into(binding.ivProfileEdit)
                }
                android.util.Log.d(TAG, "선택된 이미지 소스로 바텀시트 이미지 설정 완료")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "바텀시트에서 이미지 로드 중 오류", e)
            }
        } else {
            android.util.Log.d(TAG, "선택된 이미지 소스가 없어 서버 정보를 로드합니다")
            loadProfile()
        }
        
        setupListeners()
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
        // 디버깅 로그 추가
        android.util.Log.d(TAG, "서버에서 프로필 이미지 로드 시작")
        
        // 서버에서 사용자 정보 불러오기
        viewModel.userInfo.observe(viewLifecycleOwner) { userInfo: UserInfo? ->
            userInfo?.let {
                originalImageUrl = it.profileImageUrl
                android.util.Log.d(TAG, "서버에서 받은 프로필 이미지 URL: ${it.profileImageUrl}")
                
                if (it.profileImageUrl?.isNotEmpty() == true) {
                    try {
                        Glide.with(requireContext())
                            .load(it.profileImageUrl)
                            .centerCrop()
                            .skipMemoryCache(true)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                            .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis().toString()))
                            .placeholder(R.drawable.img_profile_default)
                            .error(R.drawable.img_profile_default)
                            .into(binding.ivProfileEdit)
                        
                        // 성공적으로 로드된 URL을 로컬에 캐시 저장
                        val preferencesUtil = PreferencesUtil(requireContext())
                        preferencesUtil.setString("user_profile_image_url", it.profileImageUrl)
                        android.util.Log.d(TAG, "서버 URL을 로컬에 캐시 저장: ${it.profileImageUrl}")
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "서버 URL 로드 중 오류 발생", e)
                        binding.ivProfileEdit.setImageResource(R.drawable.img_profile_default)
                    }
                } else {
                    android.util.Log.d(TAG, "서버에서 이미지 URL이 없어 기본 이미지 사용")
                    binding.ivProfileEdit.setImageResource(R.drawable.img_profile_default)
                }
            } ?: run {
                android.util.Log.d(TAG, "서버에서 사용자 정보가 null이어서 기본 이미지 사용")
                binding.ivProfileEdit.setImageResource(R.drawable.img_profile_default)
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
        Glide.with(requireContext())
            .load(R.drawable.img_profile_default)
            .centerCrop()
            .skipMemoryCache(true) // 메모리 캐시 사용 안 함
            .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis()))
            .into(binding.ivProfileEdit)
            
        selectedImageUri = null
        isImageChanged = true
        
        // 임시 URI 삭제
        val preferencesUtil = PreferencesUtil(requireContext())
        preferencesUtil.removeString("user_profile_image_uri_temp")
        
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
                
                // 로컬에 저장된 URI 삭제
                val preferencesUtil = PreferencesUtil(requireContext())
                preferencesUtil.removeString("user_profile_image_uri_temp")
                preferencesUtil.removeString("user_profile_image_uri")
                
                // 현재 상태 초기화
                isImageChanged = false
                
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
        android.util.Log.d(TAG, "프로필 이미지 업로드 시도: $uri, 파일명: $fileName")
        
        try {
            // 이미지 추가 확인 (디버깅용)
            if (uri != null) {
                android.util.Log.d(TAG, "URI 유효성 확인 완료")
            } else {
                android.util.Log.e(TAG, "URI가 null이어서 업로드 실패")
                Toast.makeText(requireContext(), "이미지 업로드에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                showLoading(false)
                return
            }
            
            // 프로필 이미지 URI 저장
            val preferencesUtil = PreferencesUtil(requireContext())
            val uriString = uri.toString()
            
            // 임시 저장 - 업로드 결과에 상관없이 URI 정보 보존
            preferencesUtil.saveTempProfileImageUri(uriString)
            
            // 이미지 업로드 시작 시간 기록 (캐시 문제 방지용)
            val uploadStartTime = System.currentTimeMillis()
            preferencesUtil.setString("profile_upload_time", uploadStartTime.toString())
            
            // 뷰모델을 통해 이미지 업로드 (서버 통신)
            viewModel.uploadProfileImage(uri, fileName).observe(viewLifecycleOwner) { result ->
                showLoading(false)
                
                if (result != null && result) {
                    // 업로드 성공
                    android.util.Log.d(TAG, "프로필 이미지 업로드 성공")
                    Toast.makeText(requireContext(), "프로필 이미지가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                    
                    // 이미지 URI를 정식 저장
                    // 1. 임시 URI를 정식 URI로 복사
                    val tempUri = preferencesUtil.getTempProfileImageUri()
                    if (!tempUri.isNullOrEmpty()) {
                        preferencesUtil.saveProfileImageUri(tempUri)
                        android.util.Log.d(TAG, "임시 URI를 정식 URI로 저장: $tempUri")
                    }
                    
                    // 2. 업로드 시간 정보 추가
                    preferencesUtil.setString("profile_last_updated", System.currentTimeMillis().toString())
                    
                    // 3. 임시 URI 키 삭제 (중복 방지)
                    preferencesUtil.removeString("user_profile_image_uri_temp")
                    
                    // Glide 캐시 지우기 (이미지 즉시 적용을 위해)
                    try {
                        // 메모리 캐시 먼저 지우기 (메인 스레드에서 가능)
                        Glide.get(requireContext()).clearMemory()
                        
                        // 디스크 캐시는 백그라운드 스레드에서 지우기
                        Thread {
                            try {
                                Glide.get(requireContext()).clearDiskCache()
                                android.util.Log.d(TAG, "Glide 캐시 지우기 완료")
                            } catch (e: Exception) {
                                android.util.Log.e(TAG, "Glide 디스크 캐시 지우기 실패", e)
                            }
                        }.start()
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Glide 캐시 지우기 중 오류", e)
                    }
                    
                    // 현재 상태 초기화
                    isImageChanged = false
                    
                    // 프로필 이미지 갱신 이벤트 전달
                    (parentFragment as? ProfileImageUpdateListener)?.onProfileImageUpdated()
                    
                    // 성공 시 바텀시트 닫기
                    dismiss()
                } else {
                    // 업로드 실패
                    android.util.Log.e(TAG, "프로필 이미지 업로드 실패")
                    Toast.makeText(requireContext(), "이미지 업로드에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    
                    // 실패 시 임시 URI 부분적으로 유지 (다시 시도할 수 있도록)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "이미지 업로드 중 오류 발생", e)
            Toast.makeText(requireContext(), "이미지 업로드 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            showLoading(false)
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
        
        // 취소 시 임시 URI 삭제 (완료 버튼을 누르지 않고 닫았을 경우)
        if (isImageChanged && binding.btnSaveProfile.isEnabled) {
            // 이미지 변경이 있었지만 저장하지 않은 경우에만 임시 URI 삭제
            val preferencesUtil = PreferencesUtil(requireContext())
            preferencesUtil.removeString("user_profile_image_uri_temp")
            android.util.Log.d(TAG, "바텀시트 종료 시 임시 URI 삭제 - 완료되지 않은 변경 취소")
        }
        
        // 액티브 이미지 참조 삭제
        val preferencesUtil = PreferencesUtil(requireContext())
        preferencesUtil.removeString("active_bottomsheet_image")
        
        _binding = null
    }

    // 프로필 이미지 업데이트 리스너 인터페이스
    interface ProfileImageUpdateListener {
        fun onProfileImageUpdated()
    }

    companion object {
        const val TAG = "ProfileEditBottomSheet"
        
        // 전달받은 이미지 URI를 저장할 키
        private const val ARG_CURRENT_IMAGE_URI = "current_image_uri"
        
        fun newInstance(currentImageUri: String? = null): ProfileEditBottomSheet {
            val fragment = ProfileEditBottomSheet()
            
            // 현재 이미지 URI가 있는 경우 전달
            if (!currentImageUri.isNullOrEmpty()) {
                val args = Bundle()
                args.putString(ARG_CURRENT_IMAGE_URI, currentImageUri)
                fragment.arguments = args
                android.util.Log.d(TAG, "바텀시트에 이미지 URI 전달: $currentImageUri")
            }
            
            return fragment
        }
    }
}