package com.example.maite.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.maite.MainActivity
import com.example.maite.databinding.FragmentSocialSignupBinding
import android.content.Intent
import com.example.maite.viewmodel.LoginViewModel

/**
 * 소셜 로그인 회원가입 추가 정보 입력 Fragment
 */
class SocialSignupFragment : Fragment() {
    companion object {
        private const val TAG = "SocialSignupFragment"
        private const val ARG_EMAIL = "email"
        private const val ARG_NAME = "name"
        private const val ARG_PROVIDER = "provider"
        private const val ARG_ID_TOKEN = "idToken"
        
        fun newInstance(email: String, name: String, provider: String, idToken: String) = 
            SocialSignupFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EMAIL, email)
                    putString(ARG_NAME, name)
                    putString(ARG_PROVIDER, provider)
                    putString(ARG_ID_TOKEN, idToken)
                }
            }
    }
    
    private var _binding: FragmentSocialSignupBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: LoginViewModel
    
    private var email: String = ""
    private var name: String = ""
    private var provider: String = ""
    private var idToken: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString(ARG_EMAIL, "")
            name = it.getString(ARG_NAME, "")
            provider = it.getString(ARG_PROVIDER, "")
            idToken = it.getString(ARG_ID_TOKEN, "")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSocialSignupBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[LoginViewModel::class.java]
        
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        // 소셜 계정에서 가져온 정보로 화면 초기화
        binding.etEmail.setText(email)
        binding.etName.setText(name)
        
        // 이메일과 이름은 소셜 로그인에서 가져오므로 변경 불가
        binding.etEmail.isEnabled = false
        binding.etName.isEnabled = false
        
        binding.btnSignup.setOnClickListener {
            if (validateInputs()) {
                performSocialSignup()
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        val phoneNumber = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        
        // 전화번호 입력 확인
        if (phoneNumber.isEmpty()) {
            binding.etPhone.error = "전화번호를 입력해주세요"
            binding.etPhone.requestFocus()
            return false
        }
        
        // 전화번호 형식 검사 (간단한 정규식 사용)
        val phonePattern = "^01(?:0|1|[6-9])[0-9]{7,8}$" // 010-xxxx-xxxx 형식 (하이픈 없이)
        if (!phoneNumber.matches(phonePattern.toRegex())) {
            binding.etPhone.error = "올바른 전화번호 형식이 아닙니다 (예: 01012345678)"
            binding.etPhone.requestFocus()
            return false
        }
        
        // 주소 입력 확인
        if (address.isEmpty()) {
            binding.etAddress.error = "주소를 입력해주세요"
            binding.etAddress.requestFocus()
            return false
        }
        
        return true
    }
    
    private fun performSocialSignup() {
        val phoneNumber = binding.etPhone.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        
        // 로딩 상태 표시
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSignup.isEnabled = false
        
        // ViewModel 통해 소셜 회원가입 처리
        viewModel.completeSocialSignup(
            email = email,
            name = name,
            provider = provider,
            phoneNumber = phoneNumber,
            address = address
        )
        
        // 결과는 viewModel.socialSignupResult LiveData로 전달됨
        Log.d(TAG, "소셜 회원가입 요청 - 이메일: $email, 이름: $name, 제공자: $provider")
    }
    
    private fun observeViewModel() {
        viewModel.socialSignupResult.observe(viewLifecycleOwner) { response ->
            if (response.isSuccess) {
                Toast.makeText(requireContext(), "회원가입 성공", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "소셜 회원가입 성공: accessToken=${response.result.accessToken.take(10)}...")
                navigateToMainActivity()
            } else {
                Toast.makeText(requireContext(), 
                    "회원가입 실패: ${response.message}", 
                    Toast.LENGTH_SHORT).show()
                Log.e(TAG, "회원가입 실패: ${response.message}")
                
                // 회원가입 실패 시 버튼 활성화
                binding.btnSignup.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
            Log.e(TAG, "오류: $errorMsg")
            
            // 오류 발생 시 버튼 활성화
            binding.btnSignup.isEnabled = true
            binding.progressBar.visibility = View.GONE
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSignup.isEnabled = !isLoading
            binding.etPhone.isEnabled = !isLoading
            binding.etAddress.isEnabled = !isLoading
        }
    }
    
    private fun navigateToMainActivity() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
