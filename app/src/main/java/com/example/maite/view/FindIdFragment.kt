package com.example.maite.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.example.maite.R

class FindIdFragment : Fragment() {

    private lateinit var btnBack: ImageButton
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etAuthCode: EditText
    private lateinit var btnSendAuthLayout: ConstraintLayout
    private lateinit var authCodeFields: List<EditText>
    private lateinit var tvResendAuth: TextView
    private lateinit var btnVerifyAuthLayout: ConstraintLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_id, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        btnBack = view.findViewById(R.id.btnBack)
        etName = view.findViewById(R.id.etName)
        etEmail = view.findViewById(R.id.etEmail)
        etAuthCode = view.findViewById(R.id.etAuthCode)
        btnSendAuthLayout = view.findViewById(R.id.btnSendAuthLayout)
        tvResendAuth = view.findViewById(R.id.tvResendAuth)
        btnVerifyAuthLayout = view.findViewById(R.id.btnVerifyAuthLayout)
        
        // Initialize the 6 auth code fields
        authCodeFields = listOf(
            view.findViewById(R.id.etAuthCode1),
            view.findViewById(R.id.etAuthCode2),
            view.findViewById(R.id.etAuthCode3),
            view.findViewById(R.id.etAuthCode4),
            view.findViewById(R.id.etAuthCode5),
            view.findViewById(R.id.etAuthCode6)
        )
        
        // Set up auto-focus movement between auth code fields
        setupAuthCodeFieldsAutoFocus()
        
        // Set click listener for back button
        btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Set click listener for send authentication button
        btnSendAuthLayout.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            
            if (name.isEmpty()) {
                etName.error = "이름을 입력해주세요"
                return@setOnClickListener
            }
            
            if (email.isEmpty()) {
                etEmail.error = "이메일을 입력해주세요"
                return@setOnClickListener
            }
            
            // Implement send authentication logic here
            sendAuthenticationNumber(name, email)
        }
        
        // Set click listener for resend text
        tvResendAuth.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            
            if (name.isNotEmpty() && email.isNotEmpty()) {
                // Resend authentication number
                sendAuthenticationNumber(name, email)
            }
        }
        
        // Set click listener for verify authentication button
        btnVerifyAuthLayout.setOnClickListener {
            // Get the auth code from the 6 fields
            val authCode = buildAuthCode()
            
            if (authCode.length == 6) {
                verifyAuthentication(authCode)
            } else {
                // Show error
                // Toast.makeText(requireContext(), "인증번호 6자리를 모두 입력해주세요", Toast.LENGTH_SHORT).show()
            }
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

    companion object {
        @JvmStatic
        fun newInstance() = FindIdFragment()
    }
}