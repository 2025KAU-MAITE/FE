package com.example.maite.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.maite.R
import com.example.maite.databinding.FragmentUpdatePasswordBinding
import com.example.maite.repository.AuthRepository
import kotlinx.coroutines.launch

class UpdatePasswordFragment : Fragment() {

    private val TAG = "UpdatePasswordFragment"
    private var _binding: FragmentUpdatePasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdatePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Log the received email for debugging
        val email = arguments?.getString("email")
        val dataHolderEmail = com.example.maite.util.PasswordResetDataHolder.getEmail()
        
        Log.d(TAG, "Email received in UpdatePasswordFragment: $email")
        Log.d(TAG, "Email from DataHolder: $dataHolderEmail")
        
        if (email.isNullOrEmpty() && dataHolderEmail.isNullOrEmpty()) {
            Log.e(TAG, "No email available in either arguments or DataHolder")
        }

        // Set click listener for back button
        binding.btnBack.setOnClickListener {
            // Navigate back using the activity's onBackPressed
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        // Set click listener for update password button
        binding.btnUpdatePassword.setOnClickListener {
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etPasswordConfirm.text.toString().trim()
            
            if (password.isEmpty()) {
                binding.etPassword.error = "비밀번호를 입력해주세요"
                return@setOnClickListener
            }
            
            // 비밀번호 유효성 검사 추가
            if (password.length < 6) {
                binding.etPassword.error = "비밀번호는 최소 6자리 이상이어야 합니다"
                return@setOnClickListener
            }
            
            // 비밀번호 강도 검사 기준 완화 - 단순히 6자리 이상만 체크
            
            if (confirmPassword.isEmpty()) {
                binding.etPasswordConfirm.error = "비밀번호를 확인해주세요"
                return@setOnClickListener
            }
            
            if (password != confirmPassword) {
                binding.etPasswordConfirm.error = "비밀번호가 일치하지 않습니다"
                return@setOnClickListener
            }
            
            // Here, you would typically call your API to update the password
            updatePassword(password)
        }
    }
    
    private fun updatePassword(newPassword: String) {
        // Get email from arguments bundle
        var email = arguments?.getString("email")
        Log.d(TAG, "Arguments에서 가져온 이메일: '$email'")
        
        // If email is null or empty, try to get it from DataHolder as backup
        if (email.isNullOrEmpty()) {
            Log.w(TAG, "Email is null or empty in arguments, trying DataHolder...")
            email = com.example.maite.util.PasswordResetDataHolder.getEmail()
            Log.d(TAG, "DataHolder에서 가져온 이메일: '$email'")
        }
        
        if (email.isNullOrEmpty()) {
            Log.e(TAG, "Email is null or empty in both arguments and DataHolder")
            
            // 추가적인 디버깅 정보
            Log.e(TAG, "Arguments 번들 내용: ${arguments?.keySet()?.joinToString()}")
            Log.e(TAG, "Arguments 번들 null 여부: ${arguments == null}")
            
            // 이메일 입력을 받는 대화상자 표시 (최후의 방법)
            showEmailInputDialog(newPassword)
            return
        }
        
        Log.d(TAG, "Resetting password for email: '$email'")
        proceedWithPasswordReset(email, newPassword)
    }

    /**
     * 비밀번호 유효성 검사
     * 최소 6자
     */
    private fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }

    /**
     * 이메일 정보가 없을 때 사용자에게 직접 이메일을 입력받는 대화상자 표시
     */
    private fun showEmailInputDialog(newPassword: String) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        builder.setTitle("이메일 입력")
        builder.setMessage("비밀번호 재설정을 위해 이메일을 입력해주세요.")
        
        // 이메일 입력 필드 생성
        val input = android.widget.EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        input.hint = "example@email.com"
        builder.setView(input)
        
        // 확인 버튼 설정
        builder.setPositiveButton("확인") { _, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                // 입력된 이메일로 비밀번호 재설정 API 호출
                Log.d(TAG, "사용자가 입력한 이메일로 비밀번호 재설정 시도: '$email'")
                
                // DataHolder에도 저장
                com.example.maite.util.PasswordResetDataHolder.setEmail(email)
                
                // 비밀번호 재설정 API 호출
                proceedWithPasswordReset(email, newPassword)
            } else {
                Toast.makeText(requireContext(), "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 취소 버튼 설정
        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.cancel()
            Toast.makeText(requireContext(), "비밀번호 재설정이 취소되었습니다.", Toast.LENGTH_SHORT).show()
        }
        
        builder.show()
    }
    
    /**
     * 이메일과 비밀번호로 재설정 API 호출
     */
    private fun proceedWithPasswordReset(email: String, newPassword: String) {
        // Show loading state
        binding.progressBar.visibility = View.VISIBLE
        binding.btnUpdatePassword.isEnabled = false
        
        // Use AuthRepository to update the password
        val authRepository = AuthRepository(requireContext())
        
        lifecycleScope.launch {
            try {
                val response = authRepository.resetPassword(email, newPassword)
                
                Log.d(TAG, "Reset password API response: isSuccess=${response.isSuccess}, message=${response.message}")
                
                if (response.isSuccess) {
                    // 성공 메시지 표시
                    Log.d(TAG, "Password reset successful for email: $email")
                    Toast.makeText(requireContext(), "비밀번호가 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show()
                    
                    // 데이터 홀더에서 이메일 정보 삭제 (작업 완료)
                    com.example.maite.util.PasswordResetDataHolder.clear()
                    Log.d(TAG, "Cleared email from DataHolder after successful password reset")
                    
                    // 성공 메시지를 충분히 보여주기 위해 잠시 대기
                    kotlinx.coroutines.delay(1500)
                    
                    // Navigate back to login screen
                    if (activity is LoginActivity) {
                        (activity as LoginActivity).showLoginUI()
                        requireActivity().supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    }
                } else {
                    Toast.makeText(requireContext(), "비밀번호 변경 실패: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnUpdatePassword.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clear the email data when we're done with this fragment
        com.example.maite.util.PasswordResetDataHolder.clear()
    }

    companion object {
        private const val ARG_EMAIL = "email"
        
        @JvmStatic
        fun newInstance() = UpdatePasswordFragment()
        
        @JvmStatic
        fun newInstance(email: String): UpdatePasswordFragment {
            val fragment = UpdatePasswordFragment()
            val args = Bundle().apply {
                putString(ARG_EMAIL, email)
            }
            fragment.arguments = args
            return fragment
        }
    }
}