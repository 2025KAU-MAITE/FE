package com.example.maite.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.example.maite.R
import com.example.maite.databinding.FragmentFindPasswordBinding

class FindPasswordFragment : Fragment() {

    private var _binding: FragmentFindPasswordBinding? = null
    private val binding get() = _binding!!
    private lateinit var authCodeFields: List<EditText>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFindPasswordBinding.inflate(inflater, container, false)
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
        
        // Set click listener for back button
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Set click listener for send authentication button
        binding.btnSendAuth.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            
            if (name.isEmpty()) {
                binding.etName.error = "이름을 입력해주세요"
                return@setOnClickListener
            }
            
            if (email.isEmpty()) {
                binding.etEmail.error = "이메일을 입력해주세요"
                return@setOnClickListener
            }
            
            // Implement send authentication logic here
            sendAuthenticationNumber(name, email)
        }
        
        // Set click listener for resend text
        binding.tvResendAuth.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            
            if (name.isNotEmpty() && email.isNotEmpty()) {
                // Resend authentication number
                sendAuthenticationNumber(name, email)
            }
        }
        
        // Set click listener for verify authentication button
        binding.btnVerifyAuth.setOnClickListener {
            // Skip verification for now and immediately navigate to UpdatePasswordFragment
            navigateToUpdatePasswordFragment()
            
            /* Original code with validation - to be implemented later
            // Get the auth code from the 6 fields
            val authCode = buildAuthCode()
            
            if (authCode.length == 6) {
                verifyAuthentication(authCode)
                // Navigate to update password fragment
                navigateToUpdatePasswordFragment()
            } else {
                // Show error
                // Toast.makeText(requireContext(), "인증번호 6자리를 모두 입력해주세요", Toast.LENGTH_SHORT).show()
            }
            */
        }
    }
    
    private fun navigateToUpdatePasswordFragment() {
        val updatePasswordFragment = UpdatePasswordFragment.newInstance()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, updatePasswordFragment)
            .addToBackStack(null)
            .commit()
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
    
    private fun sendAuthenticationNumber(name: String, email: String) {
        // This is where you would implement the logic to send authentication number
        // For example, making a network request to your backend
        
        // Show a toast or snackbar to inform the user
        // Toast.makeText(requireContext(), "인증번호가 발송되었습니다.", Toast.LENGTH_SHORT).show()
    }
    
    private fun verifyAuthentication(authCode: String) {
        // This is where you would implement the verification logic
        // For example, making a network request to your backend to verify the code
        
        // Show a toast or snackbar to inform the user
        // Toast.makeText(requireContext(), "인증이 완료되었습니다.", Toast.LENGTH_SHORT).show()
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