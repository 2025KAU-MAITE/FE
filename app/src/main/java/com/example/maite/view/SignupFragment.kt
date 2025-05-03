package com.example.maite.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.maite.repository.AuthRepository
import com.example.maite.databinding.FragmentSignupBinding
import kotlinx.coroutines.launch

class SignupFragment : Fragment() {

    private val TAG = "SignupFragment" // 로깅을 위한 태그 추가

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    
    // Flag to track if email has been verified
    private var isEmailVerified = false
    
    // AuthRepository 인스턴스 생성
    private val authRepository = AuthRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up back button click listener
        binding.btnBack.setOnClickListener {
            // Navigate back to previous screen
            requireActivity().supportFragmentManager.popBackStack()
        }
        
        // Set up duplicate check button click listener
        binding.btnDuplicateCheck.setOnClickListener {
            checkEmailDuplicate()
        }
        
        // Reset email verification status when email text changes
        binding.etEmail.setOnEditorActionListener { _, _, _ -> 
            isEmailVerified = false
            false
        }
        
        binding.etEmail.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val email = binding.etEmail.text.toString().trim()
                if (email.isNotEmpty() && email != currentVerifiedEmail) {
                    isEmailVerified = false
                }
            }
        }
        
        // Set up signup button click listener
        binding.btnSignup.setOnClickListener {
            // Validate inputs
            if (validateInputs()) {
                // Proceed with signup
                handleSignup()
            }
        }
    }
    
    private var currentVerifiedEmail = ""
    
    private fun checkEmailDuplicate() {
        val email = binding.etEmail.text.toString().trim()
        
        // Email format validation
        if (email.isEmpty()) {
            binding.etEmail.error = "이메일을 입력해주세요"
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "올바른 이메일 형식이 아닙니다"
            return
        }
        
        // API 호출로 이메일 중복 확인
        binding.progressBar.visibility = View.VISIBLE
        binding.btnDuplicateCheck.isEnabled = false
        
        // 디버깅 로그 추가
        Log.d(TAG, "이메일 중복 확인 API 호출 시작: $email")
        
        lifecycleScope.launch {
            try {
                Log.d(TAG, "API 요청 전송 중...")
                val response = authRepository.checkEmailDuplicate(email)
                
                // API 응답 디버깅 로그 추가
                Log.d(TAG, "API 응답 받음: isSuccess=${response.isSuccess}, code=${response.code}, message=${response.message}")
                Log.d(TAG, "결과 데이터: duplicated=${response.result.duplicated}, message=${response.result.message}")
                
                if (response.isSuccess) {
                    val result = response.result
                    val isDuplicate = result.duplicated
                    
                    if (isDuplicate) {
                        Log.d(TAG, "중복된 이메일 감지됨")
                        Toast.makeText(
                            requireContext(),
                            "이미 사용 중인 이메일입니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.etEmail.error = "이미 사용 중인 이메일입니다"
                        isEmailVerified = false
                    } else {
                        Log.d(TAG, "사용 가능한 이메일 확인됨")
                        Toast.makeText(
                            requireContext(),
                            "사용 가능한 이메일입니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.etEmail.error = null
                        isEmailVerified = true
                        currentVerifiedEmail = email
                    }
                } else {
                    // API 응답이 실패인 경우
                    Log.e(TAG, "API 응답 실패: ${response.message}")
                    Toast.makeText(
                        requireContext(),
                        "이메일 중복 확인 실패: ${response.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    isEmailVerified = false
                }
            } catch (e: Exception) {
                // 네트워크 오류 등의 예외 처리
                Log.e(TAG, "API 호출 중 예외 발생", e)
                Toast.makeText(
                    requireContext(),
                    "네트워크 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                isEmailVerified = false
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnDuplicateCheck.isEnabled = true
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        
        // Email validation
        if (email.isEmpty()) {
            binding.etEmail.error = "이메일을 입력해주세요"
            return false
        }
        
        // Simple email format validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "올바른 이메일 형식이 아닙니다"
            return false
        }
        
        // Email duplicate check validation
        if (!isEmailVerified || email != currentVerifiedEmail) {
            binding.etEmail.error = "이메일 중복 확인을 해주세요"
            Toast.makeText(
                requireContext(),
                "이메일 중복 확인을 해주세요",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        
        // Password validation
        if (password.isEmpty()) {
            binding.etPassword.error = "비밀번호를 입력해주세요"
            return false
        }
        
        // Password length validation (minimum 6 characters)
        if (password.length < 6) {
            binding.etPassword.error = "비밀번호는 최소 6자 이상이어야 합니다"
            return false
        }
        
        // Password confirmation validation
        if (confirmPassword.isEmpty()) {
            binding.etConfirmPassword.error = "비밀번호 확인을 입력해주세요"
            return false
        }
        
        // Password matching validation
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "비밀번호가 일치하지 않습니다"
            return false
        }
        
        return true
    }
    
    private fun handleSignup() {
        // Here you would implement the actual signup process
        // For now, we'll just move to the next step in the signup flow
        
        // Navigate to profile information input screen
        val signupProfileFragment = SignupProfileFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, signupProfileFragment)
            .addToBackStack(null)
            .commit()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance() = SignupFragment()
    }
}