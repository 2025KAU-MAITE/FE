package com.example.maite.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.maite.databinding.FragmentSignupCompletionBinding
import com.example.maite.model.SignupDataHolder
import com.example.maite.repository.AuthRepository
import kotlinx.coroutines.launch


class SignupCompletionFragment : Fragment() {

    private val TAG = "SignupCompletionFragment"
    private var _binding: FragmentSignupCompletionBinding? = null
    private val binding get() = _binding!!
    
    // Repository 인스턴스
    private val authRepository by lazy { AuthRepository(requireContext()) }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupCompletionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupListeners()
    }
    
    private fun setupListeners() {
        // 가입 완료 버튼 클릭 리스너
        binding.btnSignup.setOnClickListener {
            // 최종 회원가입 처리
            completeSignup()
        }
    }
    
    private fun completeSignup() {
        // 로딩 표시 (로딩 UI가 별도로 없으므로 버튼 비활성화만 처리)
        binding.btnSignup.isEnabled = false
        
        // SignupDataHolder에서 사용자 정보 가져오기
        val email = SignupDataHolder.email
        val password = SignupDataHolder.password
        val name = SignupDataHolder.name
        val phoneNumber = SignupDataHolder.phoneNumber
        val address = SignupDataHolder.address
        
        // 데이터 유효성 검사
        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || phoneNumber.isEmpty() || address.isEmpty()) {
            Toast.makeText(requireContext(), "필수 정보가 누락되었습니다", Toast.LENGTH_SHORT).show()
            binding.btnSignup.isEnabled = true
            return
        }
        
        // API 호출로 회원가입 처리
        lifecycleScope.launch {
            try {
                Log.d(TAG, "회원가입 API 호출 시작: 이메일=$email, 이름=$name, 전화번호=$phoneNumber, 주소=$address")
                val response = authRepository.signup(email, password, name, phoneNumber, address)
                
                // 응답에서 result.registered 값 확인 (API 명세에 따라 이 값이 true면 회원가입 성공)
                if (response.result.registered) {
                    // 회원가입 성공
                    Log.d(TAG, "회원가입 성공: userId=${response.result.userId}, email=${response.result.email}")
                    Toast.makeText(requireContext(), "회원가입이 완료되었습니다", Toast.LENGTH_SHORT).show()
                    
                    // 모든 수집된 사용자 정보 초기화
                    SignupDataHolder.clear()
                    
                    // 로그인 화면으로 이동
                    navigateToLoginActivity()
                } else {
                    // 회원가입 실패
                    Log.e(TAG, "회원가입 실패: ${response.message}")
                    Toast.makeText(requireContext(), "회원가입 실패: ${response.message}", Toast.LENGTH_SHORT).show()
                    binding.btnSignup.isEnabled = true
                }
            } catch (e: Exception) {
                // 오류 처리
                Log.e(TAG, "회원가입 중 오류 발생: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "회원가입 처리 중 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                
                // 버튼 다시 활성화
                binding.btnSignup.isEnabled = true
            }
        }
    }
    
    private fun navigateToLoginActivity() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}