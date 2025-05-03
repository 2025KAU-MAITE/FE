package com.example.maite.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.maite.MainActivity
import com.example.maite.databinding.FragmentSignupCompletionBinding
import com.example.maite.model.SignupDataHolder
import com.example.maite.repository.AuthRepository
import kotlinx.coroutines.launch

class SignupCompletionFragment : Fragment() {

    private val TAG = "SignupCompletionFragment"
    private var _binding: FragmentSignupCompletionBinding? = null
    private val binding get() = _binding!!
    
    // Repository 인스턴스
    private val authRepository = AuthRepository()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupCompletionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 수집된 사용자 정보 표시
        displayUserInfo()
        
        setupListeners()
    }
    
    private fun setupListeners() {
        // Back button click listener
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        
        // SignUp button click listener
        binding.btnSignup.setOnClickListener {
            // 최종 회원가입 처리
            completeSignup()
        }
    }
    
    private fun displayUserInfo() {
        // SignupDataHolder에서 수집된 모든 사용자 정보 표시
        binding.tvEmail.text = SignupDataHolder.email
        binding.tvName.text = SignupDataHolder.name
        binding.tvPhoneNumber.text = SignupDataHolder.phoneNumber
        binding.tvAddress.text = SignupDataHolder.address
        
        // 프로필 이미지 표시 (기본 이미지 또는 사용자가 선택한 이미지)
        if (SignupDataHolder.profileImageUrl == "default_profile_image") {
            // 기본 이미지 표시
            binding.ivProfileImage.setImageResource(com.example.maite.R.drawable.default_profile)
        } else {
            try {
                // 사용자가 선택한 이미지 표시
                val imageUri = android.net.Uri.parse(SignupDataHolder.profileImageUrl)
                binding.ivProfileImage.setImageURI(imageUri)
            } catch (e: Exception) {
                // URI 변환 실패 시 기본 이미지 표시
                binding.ivProfileImage.setImageResource(com.example.maite.R.drawable.default_profile)
                Log.e(TAG, "이미지 URI 변환 실패: ${e.message}")
            }
        }
    }
    
    private fun completeSignup() {
        // 로딩 상태 표시
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSignup.isEnabled = false
        
        // TODO: 서버 API 연동 부분 구현 (현재는 서버 문제로 인해 로컬에서만 동작)
        // 지금은 API 연동 없이 로컬에서만 성공으로 처리
        lifecycleScope.launch {
            try {
                // API 호출 로직은 주석 처리하고 현재는 로컬에서만 동작
                /*
                val response = authRepository.signup(
                    email = SignupDataHolder.email,
                    password = SignupDataHolder.password,
                    name = SignupDataHolder.name,
                    phoneNumber = SignupDataHolder.phoneNumber,
                    address = SignupDataHolder.address,
                    profileImageUrl = SignupDataHolder.profileImageUrl
                )
                */
                
                // 임시로 2초 딜레이를 주어 로딩 표시
                kotlinx.coroutines.delay(2000)
                
                // 회원가입 성공 처리
                Log.d(TAG, "회원가입 성공 - 수집된 정보: 이메일=${SignupDataHolder.email}, 이름=${SignupDataHolder.name}, 전화번호=${SignupDataHolder.phoneNumber}, 주소=${SignupDataHolder.address}")
                
                Toast.makeText(requireContext(), "회원가입이 완료되었습니다", Toast.LENGTH_SHORT).show()
                
                // 모든 수집된 사용자 정보 초기화
                SignupDataHolder.clear()
                
                // 메인 화면으로 이동
                navigateToMainActivity()
            } catch (e: Exception) {
                // 오류 처리
                Log.e(TAG, "회원가입 중 오류 발생", e)
                Toast.makeText(
                    requireContext(),
                    "회원가입 처리 중 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                
                // 버튼 다시 활성화
                binding.btnSignup.isEnabled = true
            } finally {
                // 로딩 상태 종료
                binding.progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun navigateToMainActivity() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}