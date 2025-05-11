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
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
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
            // 서버 응답 처리:
            // HTTP 200 (isSuccess = true): 로그인 성공
            // HTTP 500 (isSuccess = false): 회원가입 필요함
            if (response.isSuccess) {
                // 로그인 성공 케이스 (HTTP 200)
                Toast.makeText(this, "Google 로그인 성공", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Google 로그인 성공 (HTTP 200) - 메인 화면으로 이동")
                navigateToMainActivity()
            } else {
                // 미등록 사용자 케이스 (HTTP 500)
                Log.d(TAG, "Google 계정 미등록 (HTTP 500) - 회원가입 화면으로 이동")
                
                // Google 계정 정보를 이용해 회원가입 화면으로 이동
                // 구글 계정에서 제공하는 이메일과 이름을 가져와서 전달
                val account = GoogleSignIn.getLastSignedInAccount(this)
                val email = account?.email ?: ""
                val name = account?.displayName ?: ""
                val idToken = account?.idToken ?: ""
                
                navigateToSocialSignupFragment(
                    email = email,
                    name = name,
                    provider = "GOOGLE",
                    idToken = idToken
                )
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
        try {
            // Google 로그인 옵션 설정
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_web_client_id))
                .requestEmail()
                .requestProfile()  // 프로필 정보도 요청
                .build()
            
            // Google 로그인 클라이언트 초기화
            googleSignInClient = GoogleSignIn.getClient(this, gso)
            
            // 기존 로그인 상태 체크 및 초기화
            val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this)
            if (lastSignedInAccount != null) {
                Log.d(TAG, "이전 Google 로그인 세션 발견: ${lastSignedInAccount.email}")
                // 필요에 따라 세션 초기화
                googleSignInClient.signOut().addOnCompleteListener {
                    Log.d(TAG, "이전 Google 로그인 세션 로그아웃 완료")
                }
            }
            
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
                            Log.e(TAG, "ID 토큰이 null입니다.")
                        }
                    } catch (e: ApiException) {
                        // 구글 로그인 실패 - 상세 에러 코드 확인
                        Log.e(TAG, "Google 로그인 실패: 코드=${e.statusCode}, 메시지=${e.message}", e)
                        val errorMessage = when (e.statusCode) {
                            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "로그인이 취소되었습니다."
                            GoogleSignInStatusCodes.SIGN_IN_FAILED -> "로그인에 실패했습니다."
                            GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "로그인이 진행 중입니다."
                            GoogleSignInStatusCodes.NETWORK_ERROR -> "네트워크 오류가 발생했습니다."
                            else -> "Google 로그인 오류: ${e.statusCode}"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d(TAG, "Google Sign-In 취소 또는 실패: ${result.resultCode}")
                    Toast.makeText(this, "Google 로그인이 취소되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            
            Log.d(TAG, "Google 로그인 설정 완료")
        } catch (e: Exception) {
            // 설정 중 오류 발생
            Log.e(TAG, "Google 로그인 설정 오류", e)
            Toast.makeText(this, "Google 로그인 설정 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Google 로그인 시작
    private fun signInWithGoogle() {
        try {
            // 로딩 표시
            setLoading(true)
            
            // 이전 로그인 세션 로그아웃 후 다시 로그인 시도
            googleSignInClient.signOut().addOnCompleteListener {
                // 로그아웃 완료 후 로그인 시도
                val signInIntent = googleSignInClient.signInIntent
                Log.d(TAG, "Google 로그인 화면 시작")
                googleSignInLauncher.launch(signInIntent)
                
                // 로딩 표시 해제는 로그인 결과 처리 시 수행됨
            }.addOnFailureListener { e ->
                // 로그아웃 실패
                Log.e(TAG, "Google 로그아웃 실패", e)
                Toast.makeText(this, "Google 로그인 준비 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                setLoading(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google 로그인 시작 오류", e)
            Toast.makeText(this, "Google 로그인을 시작할 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            setLoading(false)
        }
    }
    
    // 서버로 Google 토큰 인증 요청
    private fun authenticateWithServer(idToken: String, email: String?, name: String?) {
        // 전달받은 이메일과 이름을 로그로 기록
        Log.d(TAG, "Google 인증 시작: email=$email, name=$name")
        
        // 로딩 상태 표시
        setLoading(true)
        
        // ViewModel의 googleLogin 메서드 호출하여 서버에 인증 요청
        // 이 메서드는 서버 응답 코드에 따라 로그인 성공(HTTP 200) 또는 회원가입 필요(HTTP 500)를 판단
        viewModel.googleLogin(idToken)
        
        // 결과는 ViewModel observer에서 처리됨 (setupObservers 메서드에 구현)
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