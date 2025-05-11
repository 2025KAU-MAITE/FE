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
import com.example.maite.databinding.FragmentFindPasswordBinding
import com.example.maite.repository.AuthRepository
import kotlinx.coroutines.launch

class FindPasswordFragment : Fragment() {

    private val TAG = "FindPasswordFragment"
    private var _binding: FragmentFindPasswordBinding? = null
    private val binding get() = _binding!!
    private lateinit var authCodeFields: List<EditText>
    
    // SMS 인증 상태 관리
    private var isAuthSent = false
    private var isAuthVerified = false
    
    // AuthRepository 인스턴스 생성
    private val authRepository by lazy { AuthRepository(requireContext()) }
    
    // 인증된 이메일 저장 변수
    private var verifiedEmail: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFindPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the 5 auth code fields
        authCodeFields = listOf(
            binding.etAuthCode1,
            binding.etAuthCode2,
            binding.etAuthCode3,
            binding.etAuthCode4,
            binding.etAuthCode5
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
            val email = binding.etEmail.text.toString().trim()
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            
            if (name.isEmpty()) {
                binding.etName.error = "이름을 입력해주세요"
                return@setOnClickListener
            }
            
            if (email.isEmpty()) {
                binding.etEmail.error = "이메일을 입력해주세요"
                return@setOnClickListener
            }
            
            if (phoneNumber.isEmpty()) {
                binding.etPhoneNumber.error = "전화번호를 입력해주세요"
                return@setOnClickListener
            }
            
            // Validate email format
            if (!isValidEmail(email)) {
                binding.etEmail.error = "올바른 이메일 형식이 아닙니다"
                return@setOnClickListener
            }
            
            // Validate phone number format (Korean format)
            if (!isValidPhoneNumber(phoneNumber)) {
                binding.etPhoneNumber.error = "올바른 전화번호 형식이 아닙니다"
                return@setOnClickListener
            }
            
            // Send authentication number
            sendAuthNumber(name, email, phoneNumber)
        }
        
        // Set click listener for resend text
        binding.tvResendAuth.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            
            if (name.isNotEmpty() && email.isNotEmpty() && phoneNumber.isNotEmpty()) {
                // Resend authentication number
                sendAuthNumber(name, email, phoneNumber)
            }
        }
        
        // Set click listener for verify authentication button
        binding.btnVerifyAuth.setOnClickListener {
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()
            val authCode = buildAuthCode()
            
            if (authCode.length != authCodeFields.size) {
                Toast.makeText(requireContext(), "인증번호 ${authCodeFields.size}자리를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            verifyAuthCode(phoneNumber, authCode)
        }
    }
    
    private fun navigateToUpdatePasswordFragment() {
        val updatePasswordFragment = UpdatePasswordFragment.newInstance()
        
        // Pass the verified email to the UpdatePasswordFragment
        val bundle = Bundle()
        bundle.putString("email", verifiedEmail)
        updatePasswordFragment.arguments = bundle
        
        if (activity is LoginActivity) {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.login_container, updatePasswordFragment)
                .addToBackStack(updatePasswordFragment.javaClass.simpleName)
                .commit()
        }
    }

    private fun setupAuthCodeFieldsAutoFocus() {
        // Auto-move to next field after entering a digit
        for (i in 0 until authCodeFields.size - 1) {
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
    
    private fun sendAuthNumber(name: String, email: String, phoneNumber: String) {
        // Disable input fields during verification
        disableInputFields()
        
        // Show loading state
        showLoading(true)
        
        // API call to send verification code
        lifecycleScope.launch {
            try {
                Log.d(TAG, "비밀번호 찾기 인증번호 발송 API 호출 시작: 이름=$name, 이메일=$email, 전화번호=$phoneNumber")
                val response = authRepository.sendResetPasswordCode(name, email, phoneNumber)
                
                // API response log
                Log.d(TAG, "API 응답 받음: isSuccess=${response.isSuccess}, message=${response.message}")
                
                if (response.isSuccess) {
                    // Auth code sent successfully
                    Toast.makeText(requireContext(), "인증번호가 발송되었습니다", Toast.LENGTH_SHORT).show()
                    
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
                    
                    // Re-enable input fields
                    enableInputFields()
                }
            } catch (e: Exception) {
                // Network error handling
                Log.e(TAG, "API 호출 중 예외 발생", e)
                Toast.makeText(
                    requireContext(),
                    "네트워크 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Re-enable input fields
                enableInputFields()
            } finally {
                // End loading state
                showLoading(false)
            }
        }
    }
    
    private fun verifyAuthCode(phoneNumber: String, authCode: String) {
        // Show loading state
        showLoading(true)
        binding.btnVerifyAuth.isEnabled = false
        
        // API call to verify the auth code
        lifecycleScope.launch {
            try {
                Log.d(TAG, "비밀번호 찾기 인증번호 확인 API 호출 시작: 전화번호=$phoneNumber, 인증코드=$authCode")
                val response = authRepository.verifyResetPasswordCode(phoneNumber, authCode)
                
                // API response log
                Log.d(TAG, "API 응답 받음: isSuccess=${response.isSuccess}, message=${response.message}")
                
                if (response.isSuccess) {
                    // Auth code verification successful
                    Toast.makeText(requireContext(), "인증이 완료되었습니다", Toast.LENGTH_SHORT).show()
                    
                    // Update status
                    isAuthVerified = true
                    
                    // Store the verified email
                    verifiedEmail = response.result.email
                    
                    // Navigate to the UpdatePasswordFragment
                    navigateToUpdatePasswordFragment()
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
                showLoading(false)
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
        for (field in authCodeFields) {
            field.setText("")
        }
    }
    
    private fun disableInputFields() {
        binding.etName.isEnabled = false
        binding.etEmail.isEnabled = false
        binding.etPhoneNumber.isEnabled = false
        binding.btnSendAuth.isEnabled = false
    }
    
    private fun enableInputFields() {
        binding.etName.isEnabled = true
        binding.etEmail.isEnabled = true
        binding.etPhoneNumber.isEnabled = true
        binding.btnSendAuth.isEnabled = true
    }
    
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
        }
    }
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.matches(Regex("^01[0-1|6-9][0-9]{7,8}$"))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = FindPasswordFragment()
    }
}