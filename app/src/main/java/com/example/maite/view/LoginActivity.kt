package com.example.maite.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.maite.MainActivity
import com.example.maite.R
import com.example.maite.databinding.ActivityLoginBinding
import com.example.maite.repository.AuthRepository
import kotlinx.coroutines.launch
import com.example.maite.PreferencesUtil
import com.example.maite.ApiClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.example.maite.view.FindIdFragment
import com.example.maite.view.FindPasswordFragment
import com.example.maite.view.SignupFragment

class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"
    private lateinit var binding: ActivityLoginBinding
    private val authRepository by lazy { AuthRepository(this) }
    private val preferencesUtil by lazy { PreferencesUtil(this) }
    
    // Google 로그인 관련 변수
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // View binding setup
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 뒤로가기 처리 설정
        setupBackPressHandling()
        
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
    
    // Google 로그인 설정
    private fun setupGoogleSignIn() {
        try {
            Log.d(TAG, "Google 로그인 설정 시작")
            
            // Google 로그인 옵션 설정
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_web_client_id))
                .requestEmail()
                .build()
            
            Log.d(TAG, "GSO 객체 생성 완료: $gso")
            
            // Google 로그인 클라이언트 초기화
            googleSignInClient = GoogleSignIn.getClient(this, gso)
            Log.d(TAG, "Google 로그인 클라이언트 초기화 완료")
            
            // Google 로그인 결과 처리를 위한 ActivityResultLauncher 설정
            googleSignInLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                Log.d(TAG, "Google 로그인 결과 수신: resultCode=${result.resultCode}")
                
                if (result.resultCode == RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    Log.d(TAG, "로그인 결과 Task 생성 완료, 실행 중...")
                    
                    try {
                        val account = task.getResult(ApiException::class.java)
                        Log.d(TAG, "Google 계정 획득 성공")
                        
                        // 구글 계정 정보 획득 성공
                        val idToken = account?.idToken
                        val email = account?.email
                        val name = account?.displayName
                        val id = account?.id
                        
                        // 토큰 정보 상세 로깅
                        Log.d(TAG, "======== GOOGLE 로그인 성공 상세 정보 ========")
                        Log.d(TAG, "계정 ID: $id")
                        Log.d(TAG, "이메일: $email")
                        Log.d(TAG, "이름: $name")
                        Log.d(TAG, "IdToken 길이: ${idToken?.length ?: 0}자")
                        Log.d(TAG, "IdToken 앞부분: ${idToken?.take(20)}...")
                        Log.d(TAG, "IdToken 뒷부분: ...${idToken?.takeLast(20)}")
                        
                        // JWT 구조 분석 (header.payload.signature)
                        idToken?.let {
                            val parts = it.split(".")
                            if (parts.size >= 2) {
                                Log.d(TAG, "JWT 형식 확인: ${parts.size}개 부분으로 구성됨")
                                Log.d(TAG, "header: ${parts[0].take(15)}...")
                                Log.d(TAG, "payload: ${parts[1].take(15)}...")
                            } else {
                                Log.w(TAG, "JWT 형식이 아닌 토큰: 부분 개수=${parts.size}")
                            }
                        }
                        Log.d(TAG, "=======================================")
                        
                        // 성공 메시지 표시
                        val successMessage = "Google 로그인 성공: $email"
                        Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show()
                        
                    } catch (e: ApiException) {
                        // 구글 로그인 실패 - 상세 에러 코드 확인
                        Log.e(TAG, "Google 로그인 실패: 코드=${e.statusCode}, 메시지=${e.message}", e)
                        
                        // 자주 발생하는 에러 코드별 처리
                        val errorMessage = when(e.statusCode) {
                            7 -> "네트워크 오류 - 인터넷 연결을 확인해주세요."
                            10 -> "개발자 오류 - 구성이 잘못되었습니다. (SHA-1 확인 필요)"
                            12501 -> "로그인이 취소되었습니다."
                            12500 -> "로그인에 실패했습니다."
                            else -> "Google 로그인 실패: 코드=${e.statusCode}"
                        }
                        
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.d(TAG, "Google 로그인 취소됨: resultCode=${result.resultCode}")
                    Toast.makeText(this, "Google 로그인이 취소되었습니다.", Toast.LENGTH_SHORT).show()
                }
                
                // 로딩 상태 해제
                setLoading(false)
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
            
            // 현재 로그인 상태 확인
            val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this)
            if (lastSignedInAccount != null) {
                Log.d(TAG, "기존 로그인된 Google 계정 발견: ${lastSignedInAccount.email}")
                Log.d(TAG, "기존 IdToken: ${lastSignedInAccount.idToken?.take(15)}... (${lastSignedInAccount.idToken?.length ?: 0}자)")
            } else {
                Log.d(TAG, "기존 로그인된 Google 계정 없음")
            }
            
            // 로그인 시도 전 로그
            Log.d(TAG, "Google 로그인 Intent 생성 및 시작")
            
            // 로그인 시도
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Google 로그인 시작 오류", e)
            Toast.makeText(this, "Google 로그인을 시작할 수 없습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            setLoading(false)
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
        
        // 로딩 표시
        setLoading(true)
        
        // API 클라이언트 초기화 (토큰 없이)
        preferencesUtil.clearAccessToken()
        ApiClient.resetClient(this)
        
        lifecycleScope.launch {
            try {
                val response = authRepository.login(email, password)
                
                if (response.isSuccess) {
                    // 로그인 성공
                    preferencesUtil.saveAccessToken(response.result.accessToken)
                    Toast.makeText(this@LoginActivity, "로그인 성공", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    // 로그인 실패
                    Toast.makeText(this@LoginActivity, "로그인 실패: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // 예외 처리
                Log.e(TAG, "로그인 요청 중 오류 발생", e)
                Toast.makeText(this@LoginActivity, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }
    
    // 로딩 상태 표시 함수
    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.progressBar.visibility = View.VISIBLE
            binding.btnLogin.isEnabled = false
            binding.btnGoogleLogin.isEnabled = false
        } else {
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true
            binding.btnGoogleLogin.isEnabled = true
        }
    }
    
    // 뒤로가기 처리 설정
    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    Log.d(TAG, "백스택 항목 수: ${supportFragmentManager.backStackEntryCount}")
                    supportFragmentManager.popBackStack()
                    
                    // 프래그먼트가 더 이상 없으면 로그인 UI 표시
                    if (supportFragmentManager.backStackEntryCount <= 1) {
                        Log.d(TAG, "마지막 프래그먼트, 로그인 UI로 돌아갑니다")
                        // 약간의 지연을 주어 UI 전환이 제대로 이루어지도록 함
                        binding.root.postDelayed({
                            showLoginUI()
                        }, 100)
                    }
                } else {
                    // 백스택이 비어있으면 기본 동작 수행 (앱 종료)
                    Log.d(TAG, "백스택이 비어있음, 앱 종료")
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
    
    // 로그인 UI 표시
    private fun showLoginUI() {
        binding.loginUIContainer.visibility = View.VISIBLE
        binding.loginContainer.visibility = View.GONE
        
        // 로딩 상태 해제
        setLoading(false)
    }
    
    // 프래그먼트 표시
    private fun showFragment(fragment: Fragment) {
        // 로그인 UI 숨기고 프래그먼트 컨테이너 표시
        binding.loginUIContainer.visibility = View.GONE
        binding.loginContainer.visibility = View.VISIBLE
        
        // 로딩 상태 확인 및 해제
        setLoading(false)
        
        // 프래그먼트 전환
        supportFragmentManager.beginTransaction()
            .replace(R.id.login_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    // 회원가입 화면으로 이동
    private fun navigateToSignupFragment() {
        showFragment(SignupFragment())
    }
    
    // 아이디 찾기 화면으로 이동
    private fun navigateToFindIdFragment() {
        showFragment(FindIdFragment())
    }
    
    // 비밀번호 찾기 화면으로 이동
    private fun navigateToFindPasswordFragment() {
        showFragment(FindPasswordFragment())
    }
    
    // 메인 화면으로 이동
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // 로그인 액티비티 종료
    }
}
