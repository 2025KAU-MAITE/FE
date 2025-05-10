package com.example.maite.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.maite.MainActivity
import com.example.maite.R
import com.example.maite.databinding.ActivityLoginBinding
import com.example.maite.repository.AuthRepository
import kotlinx.coroutines.launch
import com.example.maite.PreferencesUtil
import com.example.maite.ApiClient
import com.example.maite.viewmodel.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"
    private lateinit var binding: ActivityLoginBinding
    private val authRepository by lazy { AuthRepository(this) }
    private val preferencesUtil by lazy { PreferencesUtil(this) }
    private lateinit var viewModel: LoginViewModel
    
    // Google 로그인 관련 변수
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // View binding setup
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // ViewModel 초기화
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
        
        // Observer 설정
        setupObservers()
        
        // Google 로그인 설정
        setupGoogleSignIn()
        
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
        
        // Google 로그인 버튼
        binding.btnGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }
    }
    
    // ViewModel 관찰자 설정
    private fun setupObservers() {
        // 일반 로그인 결과 관찰
        viewModel.loginResult.observe(this) { response ->
            if (response.isSuccess) {
                Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                navigateToMainActivity()
            } else {
                Toast.makeText(this, "로그인 실패: ${response.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "로그인 실패: ${response.message}")
            }
        }
        
        // Google 로그인 결과 관찰
        viewModel.googleLoginResult.observe(this) { response ->
            if (response.isSuccess) {
                if (response.result.isRegistered) {
                    // 이미 가입된 사용자면 메인 화면으로 이동
                    Toast.makeText(this, "Google 로그인 성공", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    // 미등록 사용자면 소셜 회원가입 추가 정보 입력 화면으로 이동
                    val email = response.result.email ?: ""
                    val name = response.result.name ?: ""
                    navigateToSocialSignupFragment(
                        email = email,
                        name = name,
                        provider = "GOOGLE",
                        idToken = response.result.idToken
                    )
                }
            } else {
                Toast.makeText(this, 
                    "Google 로그인 실패: ${response.message}", 
                    Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Google 로그인 실패: ${response.message}")
            }
        }
        
        // 에러 메시지 관찰
        viewModel.errorMessage.observe(this) { errorMsg ->
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            Log.e(TAG, "오류: $errorMsg")
        }
        
        // 로딩 상태 관찰
        viewModel.loading.observe(this) { isLoading ->
            setLoading(isLoading)
        }
    }
    
    // Google 로그인 설정
    private fun setupGoogleSignIn() {
        // Google 로그인 옵션 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))
            .requestEmail()
            .build()
            
        // Google 로그인 클라이언트 초기화
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        
        // Google 로그인 결과 처리를 위한 ActivityResultLauncher 설정
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    
                    // 구글 계정 정보 획득 성공
                    val idToken = account?.idToken
                    val email = account?.email
                    val name = account?.displayName
                    
                    Log.d(TAG, "Google 로그인 성공: $email")
                    
                    // 이 ID 토큰을 백엔드 서버로 전송하여 인증 처리
                    if (idToken != null) {
                        authenticateWithServer(idToken, email, name)
                    } else {
                        Toast.makeText(this, "Google ID 토큰을 얻지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: ApiException) {
                    // 구글 로그인 실패
                    Log.e(TAG, "Google 로그인 실패", e)
                    Toast.makeText(this, "Google 로그인 실패: ${e.statusCode}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.d(TAG, "Google Sign-In 취소 또는 실패: ${result.resultCode}")
                Toast.makeText(this, "Google 로그인이 취소되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Google 로그인 시작
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }
    
    // 서버로 Google 토큰 인증 요청
    private fun authenticateWithServer(idToken: String, email: String?, name: String?) {
        setLoading(true)
        
        lifecycleScope.launch {
            try {
                // Google 로그인 검증 API 호출
                val response = authRepository.googleLogin(idToken)
                
                if (response.isSuccess) {
                    // 서버에서 응답한 isRegistered 값 가정 (실제로는 서버 응답에 이 값이 포함되어야 함)
                    val isRegistered = true // 이 부분은 실제 서버 응답에 따라 판단해야 함
                    
                    if (isRegistered) {
                        // 이미 가입된 사용자인 경우
                        Toast.makeText(this@LoginActivity, "Google 로그인 성공", Toast.LENGTH_SHORT).show()
                        
                        // 토큰 저장
                        saveAccessToken(response.result.accessToken)
                        
                        try {
                            val userInfoResponse = authRepository.getUserInfo(response.result.accessToken)
                            if (userInfoResponse.isSuccess) {
                                preferencesUtil.saveUserInfo(
                                    userInfoResponse.result.userId,
                                    userInfoResponse.result.name,
                                    userInfoResponse.result.email
                                )
                            } else {
                                // 사용자 정보는 없지만 email로 임시 정보 저장
                                if (email != null) {
                                    preferencesUtil.saveUserInfo(
                                        userId = email.hashCode().toLong(), // 임시 ID
                                        name = name ?: "Google 사용자",
                                        email = email
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Google 사용자 정보 조회 실패", e)
                            // 사용자 정보는 없지만 email로 임시 정보 저장
                            if (email != null) {
                                preferencesUtil.saveUserInfo(
                                    userId = email.hashCode().toLong(), // 임시 ID
                                    name = name ?: "Google 사용자",
                                    email = email
                                )
                            }
                        }
                        
                        // 메인 화면으로 이동
                        navigateToMainActivity()
                    } else {
                        // 가입되지 않은 사용자인 경우 -> 회원가입 추가 정보 화면으로 이동
                        navigateToSocialSignupFragment(
                            email = email ?: "",
                            name = name ?: "",
                            provider = "GOOGLE",
                            idToken = idToken
                        )
                    }
                } else {
                    // 로그인 실패
                    Toast.makeText(this@LoginActivity, 
                        "Google 로그인 실패: ${response.message}", 
                        Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Google 인증 중 오류 발생", e)
                Toast.makeText(
                    this@LoginActivity,
                    "서버 인증 처리 중 오류가 발생했습니다: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                setLoading(false)
            }
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
        
        // 로그인 전에 기존 토큰 제거
        preferencesUtil.clearAccessToken()
        
        // API 클라이언트 초기화
        ApiClient.resetClient(this)
        
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

                    try {
                        val userInfoResponse = authRepository.getUserInfo(response.result.accessToken)
                        if (userInfoResponse.isSuccess) {
                            val preferencesUtil = PreferencesUtil(this@LoginActivity)
                            preferencesUtil.saveUserInfo(
                                userInfoResponse.result.userId,
                                userInfoResponse.result.name,
                                userInfoResponse.result.email
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "사용자 정보 조회 실패", e)
                    }
                    
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
        val preferencesUtil = PreferencesUtil(this)
        preferencesUtil.saveAccessToken(token)
        Log.d(TAG, "액세스 토큰 저장: ${token.take(10)}...")
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

    // 소셜 회원가입 화면으로 이동
    private fun navigateToSocialSignupFragment(
        email: String,
        name: String,
        provider: String,
        idToken: String
    ) {
        val socialSignupFragment = SocialSignupFragment.newInstance(
            email = email,
            name = name,
            provider = provider,
            idToken = idToken
        )
        
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, socialSignupFragment)
            .addToBackStack(null)
            .commit()
    }
}