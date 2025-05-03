package com.example.maite.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.maite.databinding.FragmentSignupProfileBinding
import com.example.maite.repository.AuthRepository
import com.example.maite.model.SignupDataHolder
import kotlinx.coroutines.launch

class SignupProfileFragment : Fragment() {

    private val TAG = "SignupProfileFragment"
    
    private var _binding: FragmentSignupProfileBinding? = null
    private val binding get() = _binding!!
    
    // Authentication flow state variables
    private var isAuthSent = false
    private var isAuthVerified = false
    
    // AuthRepository 인스턴스 생성
    private val authRepository = AuthRepository()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupListeners()
        // Initially hide authentication UI elements
        hideAuthenticationViews()
    }
    
    private fun setupListeners() {
        // Back button click listener
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        
        // Send authentication number button
        binding.btnSendAuth.setOnClickListener {
            if (validatePhoneNumber()) {
                sendAuthNumber()
            }
        }
        
        // Verify authentication button
        binding.btnVerifyAuth.setOnClickListener {
            verifyAuthNumber()
        }
        
        // Resend authentication text
        binding.tvResendAuth.setOnClickListener {
            resendAuthNumber()
        }
        
        // Continue button (after authentication is verified)
        binding.btnContinue.setOnClickListener {
            if (validateInputs()) {
                navigateToAddressScreen()
            }
        }
        
        // Authentication code field automatic focus changes
        setupAuthCodeFieldFocusChanges()
    }
    
    private fun validatePhoneNumber(): Boolean {
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()
        
        if (phoneNumber.isEmpty()) {
            binding.etPhoneNumber.error = "전화번호를 입력해주세요"
            return false
        }
        
        // Simple Korean phone number validation (10-11 digits)
        if (!phoneNumber.matches(Regex("^01[0-1|6-9][0-9]{7,8}$"))) {
            binding.etPhoneNumber.error = "올바른 전화번호 형식이 아닙니다"
            return false
        }
        
        return true
    }
    
    private fun sendAuthNumber() {
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()
        
        // 전화번호 입력 필드 비활성화 (인증 과정 중 변경 방지)
        binding.etPhoneNumber.isEnabled = false
        
        // 로딩 상태 표시
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSendAuth.isEnabled = false
        
        // API 호출로 인증번호 발송
        lifecycleScope.launch {
            try {
                Log.d(TAG, "SMS 인증번호 발송 API 호출 시작: $phoneNumber")
                val response = authRepository.sendSmsAuth(phoneNumber)
                
                // API 응답 디버깅 로그
                Log.d(TAG, "API 응답 받음: isSuccess=${response.isSuccess}, message=${response.message}")
                
                if (response.isSuccess) {
                    // 인증번호 발송 성공
                    Toast.makeText(requireContext(), "인증번호가 발송되었습니다", Toast.LENGTH_SHORT).show()
                    
                    // 인증 입력 UI 표시
                    showAuthenticationViews()
                    
                    // 상태 업데이트
                    isAuthSent = true
                    isAuthVerified = false
                    
                    // 전화번호 SignupDataHolder에 저장
                    SignupDataHolder.phoneNumber = phoneNumber
                } else {
                    // 인증번호 발송 실패
                    Toast.makeText(
                        requireContext(), 
                        "인증번호 발송 실패: ${response.message}", 
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 전화번호 입력 필드 다시 활성화
                    binding.etPhoneNumber.isEnabled = true
                }
            } catch (e: Exception) {
                // 네트워크 오류 등의 예외 처리
                Log.e(TAG, "API 호출 중 예외 발생", e)
                Toast.makeText(
                    requireContext(),
                    "네트워크 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                
                // 전화번호 입력 필드 다시 활성화
                binding.etPhoneNumber.isEnabled = true
            } finally {
                // 로딩 상태 종료
                binding.progressBar.visibility = View.GONE
                binding.btnSendAuth.isEnabled = true
            }
        }
    }
    
    private fun resendAuthNumber() {
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()
        
        // 로딩 상태 표시
        binding.progressBar.visibility = View.VISIBLE
        binding.tvResendAuth.isEnabled = false
        
        // API 호출로 인증번호 재발송
        lifecycleScope.launch {
            try {
                Log.d(TAG, "SMS 인증번호 재발송 API 호출 시작: $phoneNumber")
                val response = authRepository.sendSmsAuth(phoneNumber)
                
                if (response.isSuccess) {
                    // 인증번호 재발송 성공
                    Toast.makeText(requireContext(), "인증번호가 재발송되었습니다", Toast.LENGTH_SHORT).show()
                    
                    // 인증 코드 초기화
                    clearAuthCodeFields()
                    binding.etAuthCode1.requestFocus()
                } else {
                    // 인증번호 재발송 실패
                    Toast.makeText(
                        requireContext(), 
                        "인증번호 재발송 실패: ${response.message}", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                // 네트워크 오류 등의 예외 처리
                Log.e(TAG, "API 호출 중 예외 발생", e)
                Toast.makeText(
                    requireContext(),
                    "네트워크 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // 로딩 상태 종료
                binding.progressBar.visibility = View.GONE
                binding.tvResendAuth.isEnabled = true
            }
        }
    }
    
    private fun verifyAuthNumber() {
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()
        val authCode = getAuthCode()
        val name = binding.etName.text.toString().trim()
        
        // 인증번호가 6자리인지 확인
        if (authCode.length != 6) {
            Toast.makeText(requireContext(), "인증번호 6자리를 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 이름 검증
        if (name.isEmpty()) {
            binding.etName.error = "이름을 입력해주세요"
            return
        }
        
        // 로딩 상태 표시
        binding.progressBar.visibility = View.VISIBLE
        binding.btnVerifyAuth.isEnabled = false
        
        // API 호출로 인증번호 확인
        lifecycleScope.launch {
            try {
                Log.d(TAG, "SMS 인증번호 확인 API 호출 시작")
                val response = authRepository.verifySmsAuth(phoneNumber, authCode)
                
                // API 응답 디버깅 로그
                Log.d(TAG, "API 응답 받음: isSuccess=${response.isSuccess}, message=${response.message}")
                
                if (response.isSuccess) {
                    // 인증 성공
                    Toast.makeText(requireContext(), "인증이 완료되었습니다", Toast.LENGTH_SHORT).show()
                    
                    // 상태 업데이트
                    isAuthVerified = true
                    
                    // 인증 입력 UI 비활성화
                    disableAuthFields()
                    
                    // 이름과 전화번호 저장 후 바로 다음 화면으로 이동
                    SignupDataHolder.name = name
                    SignupDataHolder.phoneNumber = phoneNumber
                    Log.d(TAG, "이름과 전화번호 저장 완료: 이름=$name, 전화번호=$phoneNumber")
                    
                    // 다음 화면으로 이동
                    navigateToAddressScreen()
                } else {
                    // 인증 실패
                    Toast.makeText(
                        requireContext(), 
                        "인증번호가 일치하지 않습니다", 
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 인증 코드 초기화
                    clearAuthCodeFields()
                    binding.etAuthCode1.requestFocus()
                }
            } catch (e: Exception) {
                // 네트워크 오류 등의 예외 처리
                Log.e(TAG, "API 호출 중 예외 발생", e)
                Toast.makeText(
                    requireContext(),
                    "네트워크 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // 로딩 상태 종료
                binding.progressBar.visibility = View.GONE
                binding.btnVerifyAuth.isEnabled = true
            }
        }
    }
    
    private fun disableAuthFields() {
        // 인증 관련 필드 비활성화
        binding.etAuthCode1.isEnabled = false
        binding.etAuthCode2.isEnabled = false
        binding.etAuthCode3.isEnabled = false
        binding.etAuthCode4.isEnabled = false
        binding.etAuthCode5.isEnabled = false
        binding.etAuthCode6.isEnabled = false
        binding.btnVerifyAuth.isEnabled = false
        binding.tvResendAuth.isEnabled = false
    }
    
    private fun clearAuthCodeFields() {
        binding.etAuthCode1.setText("")
        binding.etAuthCode2.setText("")
        binding.etAuthCode3.setText("")
        binding.etAuthCode4.setText("")
        binding.etAuthCode5.setText("")
        binding.etAuthCode6.setText("")
    }
    
    private fun getAuthCode(): String {
        return binding.etAuthCode1.text.toString() +
                binding.etAuthCode2.text.toString() +
                binding.etAuthCode3.text.toString() +
                binding.etAuthCode4.text.toString() +
                binding.etAuthCode5.text.toString() +
                binding.etAuthCode6.text.toString()
    }
    
    private fun setupAuthCodeFieldFocusChanges() {
        // Setup automatic focus changes for auth code fields
        binding.etAuthCode1.addTextChangedListener(createTextWatcher { binding.etAuthCode2.requestFocus() })
        binding.etAuthCode2.addTextChangedListener(createTextWatcher { binding.etAuthCode3.requestFocus() })
        binding.etAuthCode3.addTextChangedListener(createTextWatcher { binding.etAuthCode4.requestFocus() })
        binding.etAuthCode4.addTextChangedListener(createTextWatcher { binding.etAuthCode5.requestFocus() })
        binding.etAuthCode5.addTextChangedListener(createTextWatcher { binding.etAuthCode6.requestFocus() })
        // 마지막 인증 코드를 입력해도 자동으로 인증 버튼이 클릭되지 않도록 수정
        binding.etAuthCode6.addTextChangedListener(createTextWatcher { /* 아무것도 하지 않음 */ })
    }
    
    private fun createTextWatcher(onTextFilled: () -> Unit): android.text.TextWatcher {
        return object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.length == 1) {
                    onTextFilled()
                }
            }
        }
    }
    
    private fun showAuthenticationViews() {
        binding.authCodeFieldsLayout.visibility = View.VISIBLE
        binding.tvResendAuth.visibility = View.VISIBLE
        binding.btnVerifyAuth.visibility = View.VISIBLE
        
        // Completely disable the button instead of just hiding it
        binding.btnSendAuth.visibility = View.INVISIBLE
        binding.btnSendAuth.isEnabled = false
        binding.btnSendAuth.isClickable = false
    }
    
    private fun hideAuthenticationViews() {
        binding.authCodeFieldsLayout.visibility = View.GONE
        binding.tvResendAuth.visibility = View.GONE
        binding.btnVerifyAuth.visibility = View.GONE
        binding.btnContinue.visibility = View.GONE
    }
    
    private fun validateInputs(): Boolean {
        val name = binding.etName.text.toString().trim()
        
        if (name.isEmpty()) {
            binding.etName.error = "이름을 입력해주세요"
            return false
        }
        
        if (!isAuthSent || !isAuthVerified) {
            Toast.makeText(requireContext(), "전화번호 인증을 완료해주세요", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun navigateToAddressScreen() {
        // 사용자 이름 저장
        val name = binding.etName.text.toString().trim()
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()
        
        // 로컬 데이터 홀더에 이름과 전화번호 저장
        SignupDataHolder.name = name
        SignupDataHolder.phoneNumber = phoneNumber
        
        Log.d(TAG, "이름과 전화번호 저장 완료: 이름=$name, 전화번호=$phoneNumber")
        
        // Navigate to address input screen
        val signupAddressFragment = SignupAddressFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, signupAddressFragment)
            .addToBackStack(null)
            .commit()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance() = SignupProfileFragment()
    }
}