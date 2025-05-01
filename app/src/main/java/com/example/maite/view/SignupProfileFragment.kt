package com.example.maite.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.maite.databinding.FragmentSignupProfileBinding

class SignupProfileFragment : Fragment() {

    private var _binding: FragmentSignupProfileBinding? = null
    private val binding get() = _binding!!
    
    // Authentication flow state variables
    private var isAuthSent = false
    private var isAuthVerified = false
    
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
        // Simulate sending authentication code
        // In a real app, you would make an API call to send SMS
        
        // Show authentication views
        showAuthenticationViews()
        
        // Update state
        isAuthSent = true
        
        Toast.makeText(requireContext(), "인증번호가 발송되었습니다", Toast.LENGTH_SHORT).show()
        
        // 인증번호 입력창이 나타난 후 인증하기 버튼이 바로 클릭되지 않도록 이전 검증 상태 초기화
        isAuthVerified = false
    }
    
    private fun resendAuthNumber() {
        // Simulate resending authentication code
        Toast.makeText(requireContext(), "인증번호가 재발송되었습니다", Toast.LENGTH_SHORT).show()
    }
    
    private fun verifyAuthNumber(): Boolean {
        // Since API isn't implemented yet, automatically consider verification successful
        Toast.makeText(requireContext(), "인증이 완료되었습니다", Toast.LENGTH_SHORT).show()
        isAuthVerified = true
        
        // 이름이 입력되어 있는지만 확인한 후, 다음 화면으로 넘어갑니다
        val name = binding.etName.text.toString().trim()
        
        if (name.isEmpty()) {
            binding.etName.error = "이름을 입력해주세요"
            return false
        }
        
        // 이름이 올바르게 입력되었다면 다음 화면으로 이동
        navigateToAddressScreen()
        
        return true
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