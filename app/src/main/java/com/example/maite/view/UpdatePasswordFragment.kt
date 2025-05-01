package com.example.maite.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.maite.R
import com.example.maite.databinding.FragmentUpdatePasswordBinding

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
            requireActivity().supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }
    
    private fun updatePassword(newPassword: String) {
        // This is where you would implement the logic to update the password
        // For example, making a network request to your backend
        
        // Show a toast or snackbar to inform the user
        // Toast.makeText(requireContext(), "비밀번호가 성공적으로 변경되었습니다.", Toast.LENGTH_SHORT).show()
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