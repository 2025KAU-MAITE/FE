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
import com.example.maite.MainActivity


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
        setupBackPressHandling()
    }
    
    private fun setupBackPressHandling() {
        // 시스템 뒤로가기 버튼 처리
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(TAG, "시스템 뒤로가기 버튼 처리")
                if (requireActivity() is LoginActivity) {
                    val loginActivity = requireActivity() as LoginActivity
                    loginActivity.popBackStackOrShowLoginUI()
                } else {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }
        })
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
        val provider = SignupDataHolder.provider // 소셜 로그인 제공자 (GOOGLE 등)
        
        // 데이터 유효성 검사
        if (email.isEmpty() || name.isEmpty() || phoneNumber.isEmpty() || address.isEmpty()) {
            Toast.makeText(requireContext(), "필수 정보가 누락되었습니다", Toast.LENGTH_SHORT).show()
            binding.btnSignup.isEnabled = true
            return
        }
        
        // 소셜 로그인인지 일반 회원가입인지 확인
        if (provider.isNotEmpty()) {
            // 소셜 로그인 회원가입 처리
            completeSocialSignup(email, name, provider, phoneNumber, address)
        } else {
            // 일반 회원가입 처리
            if (password.isEmpty()) {
                Toast.makeText(requireContext(), "비밀번호가 누락되었습니다", Toast.LENGTH_SHORT).show()
                binding.btnSignup.isEnabled = true
                return
            }
            completeNormalSignup(email, password, name, phoneNumber, address)
        }
    }
    
    // 일반 회원가입 처리
    private fun completeNormalSignup(email: String, password: String, name: String, phoneNumber: String, address: String) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "일반 회원가입 API 호출 시작: 이메일=$email, 이름=$name, 전화번호=$phoneNumber, 주소=$address")
                val response = authRepository.signup(email, password, name, phoneNumber, address)
                
                // 응답에서 result.registered 값 확인 (API 명세에 따라 이 값이 true면 회원가입 성공)
                if (response.result.registered) {
                    // 회원가입 성공
                    Log.d(TAG, "회원가입 성공: userId=${response.result.userId}, email=${response.result.email}")
                    
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
    
    // 소셜 로그인 회원가입 처리
    private fun completeSocialSignup(email: String, name: String, provider: String, phoneNumber: String, address: String) {
        lifecycleScope.launch {
            try {
                // ID 토큰 확인
                val idToken = SignupDataHolder.idToken
                Log.d(TAG, "소셜 로그인 회원가입 API 호출 시작: 이메일=$email, 이름=$name, 제공자=$provider")
                Log.d(TAG, "전화번호=$phoneNumber, 주소=$address")
                
                // 토큰 정보 확인
                if (idToken.isNotEmpty()) {
                    Log.d(TAG, "사용할 ID 토큰: ${idToken.take(20)}... (${idToken.length}자)")
                } else {
                    Log.e(TAG, "ID 토큰이 비어있음! 구글 로그인 정보가 제대로 저장되지 않았을 수 있음")
                }
                
                val response = authRepository.completeSocialSignup(email, name, provider, phoneNumber, address)
                
                if (response.isSuccess) {
                    // 소셜 회원가입 성공
                    Log.d(TAG, "소셜 회원가입 성공: userId=${response.result.userId}")
                    // 안전하게 accessToken 로깅 (null일 수 있음)
                    response.result.accessToken?.let { token ->
                        Log.d(TAG, "accessToken=${token.take(15)}...")
                    } ?: Log.d(TAG, "accessToken=null")
                    
                    // 모든 수집된 사용자 정보 초기화
                    SignupDataHolder.clear()
                    
                    // 요구사항 변경: 소셜 로그인도 회원가입 성공 후 로그인 화면으로 이동
                    // 기존: 메인 화면으로 바로 이동 (소셜 로그인은 바로 로그인 상태가 됨)
                    navigateToLoginActivity()
                } else {
                    // 소셜 회원가입 실패
                    Log.e(TAG, "소셜 회원가입 실패: ${response.message}")
                    
                    // 이미 존재하는 이메일인 경우 특별 처리
                    if (response.message.contains("이미 존재하는 이메일")) {
                        Toast.makeText(requireContext(), 
                            "이미 가입된 이메일입니다. 일반 로그인을 시도해보세요.", 
                            Toast.LENGTH_LONG).show()
                            
                        // 로그인 화면으로 이동
                        navigateToLoginActivity()
                    } else {
                        // 기타 오류 메시지 표시
                        Toast.makeText(requireContext(), "회원가입 실패: ${response.message}", Toast.LENGTH_SHORT).show()
                        binding.btnSignup.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                // 오류 처리
                Log.e(TAG, "소셜 회원가입 중 오류 발생: ${e.message}", e)
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
    
    private fun navigateToMainActivity() {
        val intent = Intent(requireContext(), com.example.maite.MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}