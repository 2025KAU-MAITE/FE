package com.example.maite.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.maite.MainActivity
import com.example.maite.databinding.ActivityLoginBinding
import com.example.maite.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"
    private lateinit var binding: ActivityLoginBinding
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // View binding setup
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 로그인 버튼 클릭 이벤트
        binding.btnLogin.setOnClickListener {
            // 입력 데이터 유효성 검사 및 로그인 실행
            if (validateInputs()) {
                performLogin()
            }
        }
        
        // 아이디 찾기 이동
        binding.tvFindId.setOnClickListener {
            navigateToFindIdFragment()
        }
        
        // 비밀번호 찾기 이동
        binding.tvFindPassword.setOnClickListener {
            navigateToFindPasswordFragment()
        }
        
        // 회원가입 이동
        binding.tvSignUp.setOnClickListener {
            navigateToSignupFragment()
        }
        
        // Google 로그인 버튼 (임시로 MainActivity로 직접 이동)
        binding.btnGoogleLogin.setOnClickListener {
            // TODO: 실제 Google 로그인 구현
            Toast.makeText(this, "Google 로그인 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 입력 데이터 유효성 검사
    private fun validateInputs(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        // 이메일 입력 확인
        if (email.isEmpty()) {
            binding.etEmail.error = "이메일을 입력해주세요"
            binding.etEmail.requestFocus()
            return false
        }
        
        // 이메일 형식 검사
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "올바른 이메일 형식이 아닙니다"
            binding.etEmail.requestFocus()
            return false
        }
        
        // 비밀번호 입력 확인
        if (password.isEmpty()) {
            binding.etPassword.error = "비밀번호를 입력해주세요"
            binding.etPassword.requestFocus()
            return false
        }
        
        return true
    }
    
    // 로그인 수행 함수
    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        // 로딩 표시 (별도의 프로그래스바가 없을 경우 버튼 비활성화로 처리)
        setLoading(true)
        
        lifecycleScope.launch {
            try {
                Log.d(TAG, "로그인 시도: $email")
                val response = authRepository.login(email, password)
                
                Log.d(TAG, "로그인 응답: isSuccess=${response.isSuccess}, message=${response.message}")
                
                if (response.isSuccess) {
                    // 로그인 성공
                    Toast.makeText(this@LoginActivity, "로그인 성공", Toast.LENGTH_SHORT).show()
                    
                    // 토큰 저장 등의 처리
                    saveAccessToken(response.result.accessToken)
                    
                    // MainActivity로 이동
                    navigateToMainActivity()
                } else {
                    // 로그인 실패
                    Toast.makeText(this@LoginActivity, 
                        "로그인 실패: ${response.message}", 
                        Toast.LENGTH_SHORT).show()
                    
                    Log.e(TAG, "로그인 실패: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "로그인 중 오류 발생", e)
                Toast.makeText(this@LoginActivity, 
                    "로그인 처리 중 오류가 발생했습니다: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }
    
    // 로딩 상태 설정
    private fun setLoading(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }
    
    // 액세스 토큰 저장 (실제 구현에서는 SharedPreferences나 암호화된 저장소 사용)
    private fun saveAccessToken(token: String) {
        // TODO: 안전한 저장소에 토큰 저장 구현
        Log.d(TAG, "액세스 토큰 저장: ${token.take(10)}...")
        
        // 예시) SharedPreferences에 저장
        val sharedPref = getSharedPreferences("maite_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("access_token", token)
            apply()
        }
    }
    
    // MainActivity로 이동
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        // 백스택에서 모든 액티비티 제거하고 새로 시작
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    // 아이디 찾기 화면으로 이동
    private fun navigateToFindIdFragment() {
        val findIdFragment = FindIdFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, findIdFragment)
            .addToBackStack(null)
            .commit()
    }
    
    // 비밀번호 찾기 화면으로 이동
    private fun navigateToFindPasswordFragment() {
        val findPasswordFragment = FindPasswordFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, findPasswordFragment)
            .addToBackStack(null)
            .commit()
    }
    
    // 회원가입 화면으로 이동
    private fun navigateToSignupFragment() {
        val signupFragment = SignupFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, signupFragment)
            .addToBackStack(null)
            .commit()
    }
}