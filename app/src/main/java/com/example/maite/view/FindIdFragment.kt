package com.example.maite.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.maite.R
import com.example.maite.databinding.FragmentFindIdBinding
import com.example.maite.repository.AuthRepository
import kotlinx.coroutines.launch

class FindIdFragment : Fragment() {

    private val TAG = "FindIdFragment"
    private var _binding: FragmentFindIdBinding? = null
    private val binding get() = _binding!!
    private lateinit var authCodeFields: List<EditText>
    
    // SMS 인증 상태 관리
    private var isAuthSent = false
    private var isAuthVerified = false
    
    // AuthRepository 인스턴스 생성
    private val authRepository by lazy { AuthRepository(requireContext()) }
    
    // 찾은 이메일 저장 변수
    private var foundEmail: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFindIdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the 6 auth code fields
        authCodeFields = listOf(
            binding.etAuthCode1,
            binding.etAuthCode2,
            binding.etAuthCode3,
            binding.etAuthCode4,
            binding.etAuthCode5,
            binding.etAuthCode6
        )
        
        // Set up auto-focus movement between auth code fields
        setupAuthCodeFieldsAutoFocus()
        
        // Initially hide authentication UI elements
        hideAuthenticationViews()
        
        // Set click listener for back button
        binding.btnBack.setOnClickListener {
            // Navigate back using the activity's onBackPressed
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Set click listener for send authentication button
        binding.btnSendAuth.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            
            if (name.isEmpty()) {
                binding.etName.error = "이름을 입력해주세요"
                return@setOnClickListener
            }
            
            if (phoneNumber.isEmpty()) {
                binding.etPhoneNumber.error = "전화번호를 입력해주세요"
                return@setOnClickListener
            }
            
            // Validate phone number format (Korean format)
            if (!phoneNumber.matches(Regex("^01[0-1|6-9][0-9]{7,8}$"))) {
                binding.etPhoneNumber.error = "올바른 전화번호 형식이 아닙니다"
                return@setOnClickListener
            }
            
            // Send authentication number
            sendAuthNumber(phoneNumber)
        }
        
        // Set click listener for resend text
        binding.tvResendAuth.setOnClickListener {
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            
            if (phoneNumber.isNotEmpty()) {
                // Resend authentication number
                resendAuthNumber(phoneNumber)
            }
        }
        
        // Set click listener for verify authentication button
        binding.btnVerifyAuth.setOnClickListener {
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            val authCode = buildAuthCode()
            
            if (authCode.length != 6) {
                Toast.makeText(requireContext(), "인증번호 6자리를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            verifyAuthCode(phoneNumber, authCode)
        }
    }

    private fun navigateToEmailFoundFragment() {
        val emailFoundFragment = EmailFoundFragment.newInstance()
        
        // Pass the found email to the EmailFoundFragment
        val bundle = Bundle()
        bundle.putString("userId", foundEmail)
        emailFoundFragment.arguments = bundle
        
        if (activity is LoginActivity) {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.login_container, emailFoundFragment)
                .addToBackStack(emailFoundFragment.javaClass.simpleName)
                .commit()
        }
    }

    private fun setupAuthCodeFieldsAutoFocus() {
        // Auto-move to next field after entering a digit
        for (i in 0 until 5) {
            val currentField = authCodeFields[i]
            val nextField = authCodeFields[i + 1]
            
            currentField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1) {
                        nextField.requestFocus()
                    }
                }
            })
        }
    }
    
    private fun buildAuthCode(): String {
        val stringBuilder = StringBuilder()
        
        for (field in authCodeFields) {
            stringBuilder.append(field.text.toString())
        }
        
        return stringBuilder.toString()
    }
    
    private fun sendAuthNumber(phonenumber: String) {
        // Disable phone number field during verification
        binding.etPhoneNumber.isEnabled = false
        
        // Get name from input field
        val name = binding.etName.text.toString().trim()
        
        // Show loading state
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSendAuth.isEnabled = false
        
        // API call to send verification code
        lifecycleScope.launch {
            try {
                Log.d(TAG, "아이디 찾기 SMS 인증번호 발송 API 호출 시작: 이름=$name, 전화번호=$phonenumber")
                val response = authRepository.sendFindIdSmsAuth(name, phonenumber)
                
                // API response log
                Log.d(TAG, "API 응답 받음: isSuccess=${response.isSuccess}, message=${response.message}")
                
                if (response.isSuccess) {
                    // Auth code sent successfully
                    Log.d(TAG, "아이디 찾기 인증번호 발송 성공")
                    
                    // Show authentication input UI
                    showAuthenticationViews()
                    
                    // Update status
                    isAuthSent = true
                    isAuthVerified = false
                } else {
                    // 일반적인 실패 메시지
                    Toast.makeText(
                        requireContext(), 
                        "인증번호 발송 실패: ${response.message}", 
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Re-enable phone number field
                    binding.etPhoneNumber.isEnabled = true
                }
            } catch (e: Exception) {
                // Network error handling
                Log.e(TAG, "API 호출 중 예외 발생", e)
                Toast.makeText(
                    requireContext(),
                    "네트워크 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Re-enable phone number field
                binding.etPhoneNumber.isEnabled = true
            } finally {
                // End loading state
                binding.progressBar.visibility = View.GONE
                binding.btnSendAuth.isEnabled = true
            }
        }
    }
    
    private fun resendAuthNumber(phoneNumber: String) {
        // Show loading state
        binding.progressBar.visibility = View.VISIBLE
        binding.tvResendAuth.isEnabled = false
        
        // Get name from input field
        val name = binding.etName.text.toString().trim()
        
        // API call to resend verification code
        lifecycleScope.launch {
            try {
                Log.d(TAG, "아이디 찾기 SMS 인증번호 재발송 API 호출 시작: 이름=$name, 전화번호=$phoneNumber")
                val response = authRepository.sendFindIdSmsAuth(name, phoneNumber)
                
                if (response.isSuccess) {
                    // Auth code resent successfully
                    Log.d(TAG, "아이디 찾기 인증번호 재발송 성공")
                    
                    // Clear auth code fields
                    clearAuthCodeFields()
                    binding.etAuthCode1.requestFocus()
                } else {
                    // 일반적인 실패 메시지
                    Toast.makeText(
                        requireContext(), 
                        "인증번호 재발송 실패: ${response.message}", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                // Network error handling
                Log.e(TAG, "API 호출 중 예외 발생", e)
                Toast.makeText(
                    requireContext(),
                    "네트워크 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // End loading state
                binding.progressBar.visibility = View.GONE
                binding.tvResendAuth.isEnabled = true
            }
        }
    }
    
    private fun verifyAuthCode(phoneNumber: String, authCode: String) {
        // Show loading state
        binding.progressBar.visibility = View.VISIBLE
        binding.btnVerifyAuth.isEnabled = false
        
        // Get name from input field
        val name = binding.etName.text.toString().trim()
        
        // API call to verify the auth code
        lifecycleScope.launch {
            try {
                Log.d(TAG, "아이디 찾기 인증번호 확인 API 호출 시작: 이름=$name, 전화번호=$phoneNumber, 인증코드=$authCode")
                val response = authRepository.verifyFindId(name, phoneNumber, authCode)
                
                // API response log
                Log.d(TAG, "API 응답 받음: isSuccess=${response.isSuccess}, message=${response.message}")
                
                if (response.isSuccess) {
                    // Auth code verification successful
                    Log.d(TAG, "아이디 찾기 인증번호 검증 성공")
                    
                    // Update status
                    isAuthVerified = true
                    
                    // Store the found email
                    foundEmail = response.result.email
                    
                    // Navigate to the EmailFoundFragment
                    navigateToEmailFoundFragment()
                } else {
                    // 일반적인 실패 메시지
                    Toast.makeText(
                        requireContext(), 
                        "인증번호가 일치하지 않습니다: ${response.message}", 
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Clear auth code fields
                    clearAuthCodeFields()
                    binding.etAuthCode1.requestFocus()
                }
            } catch (e: Exception) {
                // Network error handling
                Log.e(TAG, "API 호출 중 예외 발생", e)
                Toast.makeText(
                    requireContext(),
                    "네트워크 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // End loading state
                binding.progressBar.visibility = View.GONE
                binding.btnVerifyAuth.isEnabled = true
            }
        }
    }
    
    private fun showAuthenticationViews() {
        binding.authCodeFieldsLayout.visibility = View.VISIBLE
        binding.tvResendAuth.visibility = View.VISIBLE
        binding.btnVerifyAuth.visibility = View.VISIBLE
    }
    
    private fun hideAuthenticationViews() {
        binding.authCodeFieldsLayout.visibility = View.GONE
        binding.tvResendAuth.visibility = View.GONE
        binding.btnVerifyAuth.visibility = View.GONE
    }
    
    private fun clearAuthCodeFields() {
        binding.etAuthCode1.setText("")
        binding.etAuthCode2.setText("")
        binding.etAuthCode3.setText("")
        binding.etAuthCode4.setText("")
        binding.etAuthCode5.setText("")
        binding.etAuthCode6.setText("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = FindIdFragment()
    }
}