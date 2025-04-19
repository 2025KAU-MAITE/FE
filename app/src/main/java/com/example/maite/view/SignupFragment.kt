package com.example.maite.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.maite.databinding.FragmentSignupBinding

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    
    // Flag to track if email has been verified
    private var isEmailVerified = false

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
        
        // Simulate network call to check for duplicate email
        // In a real app, you would make an API call to your backend
        simulateEmailDuplicateCheck(email)
    }
    
    private fun simulateEmailDuplicateCheck(email: String) {
        // For demo purposes, we'll consider "test@example.com" as already registered
        val isDuplicate = email == "test@example.com"
        
        if (isDuplicate) {
            Toast.makeText(
                requireContext(),
                "이미 사용 중인 이메일입니다.",
                Toast.LENGTH_SHORT
            ).show()
            binding.etEmail.error = "이미 사용 중인 이메일입니다"
            isEmailVerified = false
        } else {
            Toast.makeText(
                requireContext(),
                "사용 가능한 이메일입니다.",
                Toast.LENGTH_SHORT
            ).show()
            binding.etEmail.error = null
            isEmailVerified = true
            currentVerifiedEmail = email
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
        // For now, we'll just show a toast message
        Toast.makeText(
            requireContext(),
            "회원가입이 완료되었습니다",
            Toast.LENGTH_SHORT
        ).show()
        
        // Navigate to login screen or main screen after successful signup
        requireActivity().supportFragmentManager.popBackStack()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        fun newInstance() = SignupFragment()
    }
}