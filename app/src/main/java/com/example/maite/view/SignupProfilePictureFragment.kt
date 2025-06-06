package com.example.maite.view

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.maite.R
import com.example.maite.model.SignupDataHolder
import com.example.maite.repository.UserRepository
import com.example.maite.ui.profile.ProfileViewModel
import java.util.UUID

class SignupProfilePictureFragment : Fragment() {

    private val TAG = "SignupProfilePictureFragment"
    private var selectedImageUri: Uri? = null
    
    // API 통신을 위한 ViewModel 및 Repository
    private lateinit var userRepository: UserRepository
    private lateinit var profileViewModel: ProfileViewModel
    
    // UI 요소들
    private lateinit var ivProfilePicture: ImageView
    private lateinit var tvAddPicture: TextView
    private lateinit var btnContinue: Button
    private lateinit var btnSkip: Button
    private lateinit var btnBack: ImageButton
    
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                ivProfilePicture.setImageURI(uri)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup_profile_picture, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // UI 요소 초기화
        initViews(view)
        
        // Repository 초기화
        userRepository = UserRepository(requireContext())
        
        // ViewModel 초기화
        profileViewModel = ProfileViewModel(requireActivity().application)
        
        setupListeners()
        setupBackPressHandling()
    }
    
    private fun initViews(view: View) {
        ivProfilePicture = view.findViewById(R.id.ivProfilePicture)
        tvAddPicture = view.findViewById(R.id.tvAddPicture)
        btnContinue = view.findViewById(R.id.btnContinue)
        btnSkip = view.findViewById(R.id.btnSkip)
        btnBack = view.findViewById(R.id.btnBack)
    }
    
    private fun setupBackPressHandling() {
        // 시스템 뒤로가기 버튼 처리
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "시스템 뒤로가기 버튼 처리")
                if (requireActivity() is LoginActivity) {
                    val loginActivity = requireActivity() as LoginActivity
                    loginActivity.popBackStackOrShowLoginUI()
                } else {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        })
    }
    
    private fun setupListeners() {
        // Back button click listener
        btnBack.setOnClickListener {
            Log.d(TAG, "뒤로가기 버튼 클릭")
            // 로그인 화면으로 돌아가기 위해 LoginActivity의 메서드 호출
            if (requireActivity() is LoginActivity) {
                val loginActivity = requireActivity() as LoginActivity
                loginActivity.popBackStackOrShowLoginUI()
            } else {
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
        
        // Profile picture click listener
        ivProfilePicture.setOnClickListener {
            openImagePicker()
        }
        
        // Add picture text click listener
        tvAddPicture.setOnClickListener {
            openImagePicker()
        }
        
        // Skip button click listener
        btnSkip.setOnClickListener {
            // 기본 이미지 설정 (프로필 기본 사람 모양 사진)
            saveDefaultProfilePicture()
            navigateToNextScreen()
        }
        
        // Continue button click listener (previously Add button)
        btnContinue.setOnClickListener {
            // 선택한 이미지가 있으면 업로드하고 다음 화면으로 이동
            if (selectedImageUri != null) {
                saveSelectedProfilePicture()
                // navigateToNextScreen()은 saveSelectedProfilePicture() 내부에서 호출됨
            } else {
                Toast.makeText(requireContext(), "프로필 사진을 선택해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }
    
    /**
     * 건너뛰기 버튼 클릭 시 기본 프로필 이미지를 저장
     */
    private fun saveDefaultProfilePicture() {
        // null을 저장하여 기본 프로필 이미지 사용을 나타냄
        SignupDataHolder.profileImageUrl = ""
        Log.d(TAG, "기본 프로필 이미지 설정 (null 저장)")
        Toast.makeText(requireContext(), "기본 프로필 이미지가 설정되었습니다", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 사용자가 선택한 프로필 이미지를 저장
     */
    private fun saveSelectedProfilePicture() {
        selectedImageUri?.let { uri ->
            // 로딩 표시
            showLoading(true)
            
            // 파일명 생성
            val fileName = "profile_${UUID.randomUUID()}.jpg"
            Log.d(TAG, "프로필 이미지 업로드 시작: $uri, 파일명: $fileName")
            
            // AuthAPI를 통해 이미지 업로드 (회원가입 전용 엔드포인트 사용)
            profileViewModel.uploadSignupProfileImage(uri, fileName).observe(viewLifecycleOwner) { result ->
                showLoading(false)
                
                val success = result.first
                val imageUrl = result.second
                
                if (success) {
                    // 업로드 성공
                    if (!imageUrl.isNullOrEmpty()) {
                        SignupDataHolder.profileImageUrl = imageUrl
                        Log.d(TAG, "회원가입 프로필 이미지 업로드 및 URL 저장 완료: $imageUrl")
                    } else {
                        // 업로드는 성공했지만 URL을 못 받은 경우
                        SignupDataHolder.profileImageUrl = ""
                        Log.d(TAG, "회원가입 프로필 이미지 업로드 성공, URL은 받지 못함")
                    }
                    navigateToNextScreen()
                } else {
                    Log.e(TAG, "회원가입 프로필 이미지 업로드 실패")
                    Toast.makeText(requireContext(), "이미지 업로드에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * 로딩 상태 표시/숨김
     */
    private fun showLoading(show: Boolean) {
        if (show) {
            btnContinue.isEnabled = false
            btnSkip.isEnabled = false
            btnContinue.text = "업로드 중..."
        } else {
            btnContinue.isEnabled = true
            btnSkip.isEnabled = true
            btnContinue.text = "추가하기"
        }
    }

    private fun navigateToNextScreen() {
        // Navigate to the signup completion screen
        val signupCompletionFragment = SignupCompletionFragment()
        
        // LoginActivity의 FragmentContainer를 사용
        if (requireActivity() is LoginActivity) {
            val loginActivity = requireActivity() as LoginActivity
            loginActivity.navigateToProfileFragment(signupCompletionFragment)
        } else {
            Log.e(TAG, "Activity가 LoginActivity가 아닙니다!")
            Toast.makeText(requireContext(), "오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }
}
