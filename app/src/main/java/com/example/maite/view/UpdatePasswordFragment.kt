package com.example.maite.view

import android.os.Bundle
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
            
            // Navigate back to login screen
            if (activity is LoginActivity) {
                (activity as LoginActivity).showLoginUI()
                requireActivity().supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
        }
    }
    
    private fun updatePassword(newPassword: String) {
        // Get email from arguments bundle
        val email = arguments?.getString("email")
        
        if (email.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "이메일 정보가 없습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading state
        binding.progressBar.visibility = View.VISIBLE
        binding.btnUpdatePassword.isEnabled = false
        
        // Use AuthRepository to update the password
        val authRepository = AuthRepository(requireContext())
        
        lifecycleScope.launch {
            try {
                val response = authRepository.resetPassword(email, newPassword)
                
                if (response.isSuccess) {
                    Toast.makeText(requireContext(), "비밀번호가 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show()
                    
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

    companion object {
        @JvmStatic
        fun newInstance() = UpdatePasswordFragment()
    }
}