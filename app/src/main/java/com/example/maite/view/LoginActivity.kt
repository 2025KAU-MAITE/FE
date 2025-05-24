package com.example.maite.view

/**
 * Login Activity handling both standard email/password login and Google Sign-In.
 * 
 * This implementation uses a simplified and modern approach for Google Sign-In.
 */

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
import com.example.maite.BuildConfig
import com.example.maite.R
import com.example.maite.databinding.ActivityLoginBinding
import com.example.maite.repository.AuthRepository
import kotlinx.coroutines.launch
import com.example.maite.PreferencesUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.gson.annotations.SerializedName
import com.google.gson.Gson
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.maite.model.SignupDataHolder
import com.example.maite.network.WebSocketManager

class LoginActivity : AppCompatActivity() {
    // 필요한 상수
    private val ERROR_NETWORK = 7
    private val ERROR_CANCELED = 12501
    private val GOOGLE_PLAY_SERVICES_REQUEST_CODE = 9000

    private val TAG = "LoginActivity"
    private lateinit var binding: ActivityLoginBinding
    private val authRepository by lazy { AuthRepository(this) }
    private val preferencesUtil by lazy { PreferencesUtil(this) }
    
    // SharedPreferences keys
    private val KEY_USER_EMAIL = "user_email"
    
    // Google Sign-In related variables
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // View binding setup
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup back press handling
        setupBackPressHandling()
        
        // Initialize Google Sign-In
        initializeGoogleSignIn()
        
        // Set up click listeners
        setupClickListeners()
    }
    
    /**
     * Set up all click listeners for the view
     */
    private fun setupClickListeners() {
        // Login button
        binding.btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin()
            }
        }
        
        // Find ID
        binding.tvFindId.setOnClickListener {
            navigateToFindIdFragment()
        }
        
        // Find password
        binding.tvFindPassword.setOnClickListener {
            navigateToFindPasswordFragment()
        }
        
        // Sign up
        binding.tvSignUp.setOnClickListener {
            navigateToSignupFragment()
        }
        
        // Google login
        binding.btnGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }
        
        // Remove debug functionality from long clicks
        binding.btnGoogleLogin.setOnLongClickListener {
            // 디버그 기능 제거
            true
        }
    }
    
    /**
     * Shows the login UI
     * Called from other fragments
     */
    fun showLoginUI() {
        Log.d(TAG, "showLoginUI 호출됨: 백스택 수: ${supportFragmentManager.backStackEntryCount}")
        logBackStackState()
        
        try {
            // Clear fragment backstack
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                Log.d(TAG, "모든 백스택 제거 완료")
            }
        } catch (e: Exception) {
            Log.e(TAG, "백스택 클리어 중 오류 발생", e)
        }
        
        // Show login UI
        if (::binding.isInitialized) {
            binding.loginContainer.visibility = View.GONE
            binding.loginUIContainer.visibility = View.VISIBLE
            Log.d(TAG, "로그인 UI 표시됨")
        } else {
            Log.e(TAG, "binding이 초기화되지 않음")
        }
    }
    
    /**
     * Shows the fragment container
     * Referenced from other fragments
     */
    fun showFragmentContainer() {
        if (::binding.isInitialized) {
            binding.loginContainer.visibility = View.VISIBLE
            binding.loginUIContainer.visibility = View.GONE
        }
    }
    
    /**
     * Initialize Google Sign-In
     * Sets up the GoogleSignInClient and ActivityResultLauncher
     */
    private fun initializeGoogleSignIn() {
        try {
            // Get web client ID from BuildConfig
            val webClientId = BuildConfig.GOOGLE_CLIENT_ID
            
            // Configure Google Sign-In options
            // Note: GoogleSignInOptions is marked as deprecated but still the recommended way 
            // until Google Identity Services fully replaces it
            @Suppress("DEPRECATION")
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)  // 서버 확인용 ID 토큰 요청
                .requestEmail()  // 이메일 정보 요청
                // 필요한 스코프 명시적으로 추가
                .requestScopes(
                    Scope("email"), 
                    Scope("profile"),
                    Scope("https://www.googleapis.com/auth/userinfo.email"),
                    Scope("https://www.googleapis.com/auth/userinfo.profile"),
                    Scope("openid")
                )
                // 오프라인 액세스를 위한 서버 인증 코드 요청 (accessToken 획득용)
                .requestServerAuthCode(webClientId, false)  // false = 매번 새 리프레시 토큰 요청하지 않음
                .build()
            
            // Initialize Google Sign-In client
            googleSignInClient = GoogleSignIn.getClient(this, gso)
            
            // Register for activity result
            googleSignInLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                // 로딩 상태 종료
                setLoading(false)
                
                try {
                    val data = result.data
                    
                    if (result.resultCode == RESULT_OK && data != null) {
                        try {
                            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                            val account = task.getResult(ApiException::class.java)
                            
                            // account will never be null if getResult doesn't throw an exception
                            processGoogleSignIn(account)
                        } catch (e: ApiException) {
                            // API 예외 발생 시 에러 메시지 표시
                            val errorMessage = when (e.statusCode) {
                                ERROR_NETWORK -> "네트워크 연결을 확인해주세요"
                                ERROR_CANCELED -> "로그인이 취소되었습니다"
                                else -> "Google 로그인에 실패했습니다 (코드: ${e.statusCode})"
                            }
                            
                            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                            Log.e(TAG, "Google Sign-In API error: ${e.statusCode}", e)
                        }
                    } else {
                        // 사용자가 로그인을 취소한 경우
                        Toast.makeText(this, "로그인이 취소되었습니다", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "로그인 처리 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Google Sign-In 처리 오류", e)
                }
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Google 로그인 설정에 실패했습니다", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Google Sign-In initialization error", e)
        }
    }
    
    /**
     * Start Google Sign-In process
     * Checks Play Services, attempts silent sign-in, or launches the sign-in UI
     */
    private fun signInWithGoogle() {
        try {
            // Show loading indicator
            setLoading(true)
            
            // Check if Google Play Services is available
            if (!isGooglePlayServicesAvailable()) {
                setLoading(false)
                return
            }
            
            // Try silent sign-in first (uses existing account if available)
            googleSignInClient.silentSignIn()
                .addOnSuccessListener { account ->
                    // Silent sign-in successful, process the account
                    processGoogleSignIn(account)
                }
                .addOnFailureListener { e ->
                    // Silent sign-in failed, launch the sign-in UI
                    Log.d(TAG, "Silent sign-in failed, launching sign-in UI", e)
                    launchSignIn()
                }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Google 로그인을 시작할 수 없습니다", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Failed to start Google Sign-In", e)
            setLoading(false)
        }
    }
    
    /**
     * Launch Google Sign-In intent
     */
    private fun launchSignIn() {
        try {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        } catch (e: Exception) {
            setLoading(false)
            Toast.makeText(this, "로그인을 시작할 수 없습니다", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Failed to launch sign-in intent", e)
        }
    }
    
    /**
     * Check if Google Play Services is available
     * Handles error resolution if possible
     */
    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        
        if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                // Show dialog to resolve the error
                googleApiAvailability.getErrorDialog(this, resultCode, GOOGLE_PLAY_SERVICES_REQUEST_CODE)?.show()
            } else {
                Toast.makeText(this, "Google Sign-In은 이 기기에서 사용할 수 없습니다", Toast.LENGTH_SHORT).show()
            }
            return false
        }
        return true
    }

    /**
     * Process Google Sign-In Account
     * Sends the ID token to server and handles the response
     */
    // 토큰 응답을 위한 데이터 클래스
    private data class TokenResponse(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("expires_in") val expiresIn: Int,
        @SerializedName("token_type") val tokenType: String,
        @SerializedName("scope") val scope: String,
        @SerializedName("refresh_token") val refreshToken: String? = null
    )

    @Suppress("DEPRECATION")
    private fun processGoogleSignIn(account: GoogleSignInAccount) {
        val email = account.email ?: ""
        val idToken = account.idToken
        val name = account.displayName ?: ""
        val serverAuthCode = account.serverAuthCode
        
        if (idToken == null) {
            Toast.makeText(this, "인증 토큰을 가져올 수 없습니다", Toast.LENGTH_SHORT).show()
            setLoading(false)
            return
        }
        
        // 로그에 사용자 정보 추가
        Log.d(TAG, "Google 계정 정보: 이메일=$email, 이름=$name")
        Log.d(TAG, "ID 토큰 길이: ${idToken.length}")
        
        // serverAuthCode가 있다면 이를 사용해 accessToken 획득
        if (serverAuthCode != null) {
            Log.d(TAG, "서버 인증 코드 획득: ${serverAuthCode.take(10)}...")
            
            // accessToken을 얻기 위해 백그라운드 작업 실행
            lifecycleScope.launch {
                try {
                    val accessToken = getAccessTokenFromAuthCode(serverAuthCode)
                    if (accessToken != null) {
                        Log.d(TAG, "OAuth AccessToken 획득 성공: 길이=${accessToken.length}")
                        
                        // 개발 환경에서 토큰 검증 (디버깅 용도)
                        val isTokenValid = validateGoogleToken(idToken)
                        Log.d(TAG, "ID 토큰 유효성 검증 결과: $isTokenValid")
                        
                        // 서버 인증 진행 (idToken + accessToken)
                        sendTokenToServer(idToken, accessToken, email, name)
                    } else {
                        Log.e(TAG, "OAuth AccessToken 획득 실패")
                        // idToken만으로 서버 인증 시도
                        sendTokenToServer(idToken, "", email, name)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "OAuth 토큰 처리 중 오류 발생", e)
                    // 오류 발생 시 idToken만으로 서버 인증 시도
                    sendTokenToServer(idToken, "", email, name)
                }
            }
        } else {
            Log.w(TAG, "서버 인증 코드를 획득하지 못했습니다. ID 토큰만 사용합니다.")
            sendTokenToServer(idToken, "", email, name)
        }
    }
    
    /**
     * Google OAuth 서버 인증 코드로부터 accessToken을 획득
     */
    private suspend fun getAccessTokenFromAuthCode(authCode: String): String? = withContext(Dispatchers.IO) {
        try {
            val webClientId = BuildConfig.GOOGLE_CLIENT_ID
            val webClientSecret = BuildConfig.GOOGLE_CLIENT_SECRET
            
            // 클라이언트 ID와 시크릿 확인
            if (webClientSecret.isEmpty() || webClientSecret == "REPLACE_WITH_YOUR_CLIENT_SECRET") {
                Log.e(TAG, "Google 클라이언트 시크릿이 설정되지 않았습니다")
                return@withContext null
            }
            
            Log.d(TAG, "Google OAuth 토큰 요청: 인증 코드=${authCode.take(10)}...")
            Log.d(TAG, "클라이언트 ID: ${webClientId.take(15)}...")
            
            val tokenUrl = URL("https://oauth2.googleapis.com/token")
            val connection = tokenUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.doOutput = true
            
            // OAuth 2.0 토큰 요청 파라미터 생성
            val postData = "code=$authCode" +
                    "&client_id=$webClientId" +
                    "&client_secret=$webClientSecret" +
                    "&grant_type=authorization_code" +
                    "&redirect_uri=http://localhost:8080/login/oauth2/code/google"
            
            Log.d(TAG, "OAuth 요청 데이터 준비 완료")
                    
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(postData)
            writer.flush()
            
            val responseCode = connection.responseCode
            Log.d(TAG, "OAuth 응답 코드: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "OAuth 토큰 응답: $response")
                
                val tokenResponse = Gson().fromJson(response, TokenResponse::class.java)
                Log.d(TAG, "Access Token 획득 성공: ${tokenResponse.accessToken.take(15)}...")
                return@withContext tokenResponse.accessToken
            } else {
                val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                Log.e(TAG, "OAuth 토큰 오류 - HTTP $responseCode: $errorStream")
                
                // 오류 내용에 따른 처리
                if (errorStream.contains("client_secret is missing")) {
                    Log.e(TAG, "client_secret 누락 오류: Google Cloud Console에서 OAuth 클라이언트 ID를 확인하세요")
                    // client_secret 관련 오류를 보고하고 실패 처리
                }
                
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "OAuth 토큰 획득 중 오류 발생", e)
            return@withContext null
        }
    }

    /**
     * 서버에 토큰 전송 및 응답 처리
     * 서버 응답에 따라 로그인 처리 또는 회원가입 화면으로 이동
     */
    private fun sendTokenToServer(idToken: String, accessToken: String, email: String, name: String = "") {
        // 로딩 표시
        setLoading(true)
        
        lifecycleScope.launch {
            try {
                Log.d(TAG, "서버에 Google ID 토큰과 Access 토큰 전송 시작")
                Log.d(TAG, "ID 토큰 길이: ${idToken.length}, Access 토큰 길이: ${accessToken.length}")
                
                // 디버깅 용도로 토큰 정보 일부 출력
                if (idToken.length > 50) {
                    Log.d(TAG, "ID 토큰 처음/끝 부분: ${idToken.take(20)}...${idToken.takeLast(20)}")
                }
                if (accessToken.length > 50) {
                    Log.d(TAG, "Access 토큰 처음/끝 부분: ${accessToken.take(20)}...${accessToken.takeLast(20)}")
                }
                
                // 서버 API 호출 (idToken과 accessToken 모두 전달)
                val response = authRepository.googleLogin(idToken, accessToken)
                
                if (response.isSuccess) {
                    // 로그인 성공
                    Log.d(TAG, "서버 인증 성공: ${response.message}")
                    Toast.makeText(this@LoginActivity, "Google 로그인 성공", Toast.LENGTH_SHORT).show()
                    
                    // 액세스 토큰이 유효한지 확인
                    if (response.result.accessToken.isNotBlank()) {
                        // 토큰 저장
                        preferencesUtil.saveAccessToken(response.result.accessToken)
                        Log.d(TAG, "액세스 토큰 저장 완료 (길이: ${response.result.accessToken.length})")

                        //userId 저장 (5월 24일 추가)
                        preferencesUtil.saveUserId(response.result.userId)
                        
                        // 사용자 이메일 저장
                        preferencesUtil.setString(KEY_USER_EMAIL, email)

                        val webSocketManager = WebSocketManager.getInstance()
                        if (!webSocketManager.isConnected()) {
                            webSocketManager.connect()
                        }
                        
                        // 메인 화면으로 이동
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // 토큰이 비어있는 경우 (서버 응답 오류)
                        Log.e(TAG, "서버에서 빈 액세스 토큰 반환")
                        Toast.makeText(this@LoginActivity, "서버 인증 오류: 액세스 토큰 없음", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 로그인 실패 - 서버 응답 코드에 따라 처리
                    when (response.code) {
                        "NEED_SIGNUP" -> {
                            Log.d(TAG, "미등록 사용자: 회원가입 필요")
                            Toast.makeText(this@LoginActivity, "회원가입이 필요합니다", Toast.LENGTH_SHORT).show()
                            // 회원가입 화면으로 이동 (이름 정보와 accessToken 함께 전달)
                            navigateToSignupWithGoogleInfo(email, idToken, accessToken, name)
                        }
                        "USER_EXISTS" -> {
                            Log.d(TAG, "사용자 이미 존재: 다시 로그인 시도")
                            Toast.makeText(this@LoginActivity, "기존 계정이 있습니다. 다시 로그인합니다.", Toast.LENGTH_SHORT).show()
                            // 기존 사용자로 로그인 재시도 (토큰 재발급 필요할 수 있음)
                            retrySilentSignIn()
                        }
                        else -> {
                            Log.e(TAG, "서버 응답 오류: ${response.code} - ${response.message}")
                            Toast.makeText(this@LoginActivity, response.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google 로그인 서버 통신 오류", e)
                Toast.makeText(this@LoginActivity, "서버 통신 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }
    
    /**
     * Silent sign-in 재시도 (기존 사용자 로그인 처리)
     */
    private fun retrySilentSignIn() {
        try {
            setLoading(true)
            // 기존 로그인 정보 초기화
            googleSignInClient.signOut().addOnCompleteListener {
                // Silent Sign-In 다시 시도
                googleSignInClient.silentSignIn()
                    .addOnSuccessListener { account ->
                        // 새로운 토큰으로 로그인 다시 시도
                        processGoogleSignIn(account)
                    }
                    .addOnFailureListener {
                        // 실패 시 일반 로그인 UI 표시
                        setLoading(false)
                        launchSignIn()
                    }
            }
        } catch (e: Exception) {
            setLoading(false)
            Log.e(TAG, "로그인 재시도 중 오류 발생", e)
            Toast.makeText(this, "로그인 재시도에 실패했습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Navigate to Signup with Google account info
     */
    private fun navigateToSignupWithGoogleInfo(email: String, idToken: String, accessToken: String, name: String = "") {
        // Google 계정 정보 저장
        SignupDataHolder.email = email
        SignupDataHolder.provider = "GOOGLE"
        SignupDataHolder.idToken = idToken
        SignupDataHolder.accessToken = accessToken  // accessToken 추가 저장
        SignupDataHolder.name = name  // Google에서 가져온 이름 저장
        
        // 이름 로깅
        if (name.isNotEmpty()) {
            Log.d(TAG, "Google에서 가져온 이름 정보: $name")
        } else {
            Log.d(TAG, "Google에서 이름 정보를 가져오지 못했습니다")
        }
        
        // 회원가입 화면으로 이동
        val signupProfileFragment = SignupProfileFragment.newInstance()
        showFragmentContainer()
        navigateToFragment(signupProfileFragment, SignupProfileFragment::class.java.simpleName)
    }

    /**
     * Perform standard login
     */
    private fun performLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        // Show loading indicator
        setLoading(true)
        
        // Call login API
        lifecycleScope.launch {
            try {
                val loginResult = authRepository.login(email, password)
                
                if (loginResult.isSuccess) {
                    // Login successful
                    Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()
                    
                    // Save access token
                    preferencesUtil.saveAccessToken(loginResult.result.accessToken)

                    //userId 저장 (5월24일 추가)
                    preferencesUtil.saveUserId(loginResult.result.userId)
                    
                    // Save user email (use requested email for standard login)
                    preferencesUtil.setString(KEY_USER_EMAIL, email)

                    val webSocketManager = WebSocketManager.getInstance()
                    if (!webSocketManager.isConnected()) {
                        webSocketManager.connect()
                    }
                    
                    // Navigate to main activity
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Login failed - Changed to display a consistent message regardless of server response
                    Toast.makeText(this@LoginActivity, "아이디 비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Exception occurred
                Log.e(TAG, "로그인 중 오류 발생", e)
                Toast.makeText(this@LoginActivity, "아이디와 비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show()
            } finally {
                // Hide loading indicator
                setLoading(false)
            }
        }
    }
    
    /**
     * Set up back press handling
     */
    private fun setupBackPressHandling() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                popBackStackOrShowLoginUI()
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
    
    /**
     * Validate input fields
     */
    private fun validateInputs(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        // Check email field
        if (email.isEmpty()) {
            binding.etEmail.error = "이메일을 입력해주세요"
            binding.etEmail.requestFocus()
            return false
        }
        
        // Check password field
        if (password.isEmpty()) {
            binding.etPassword.error = "비밀번호를 입력해주세요"
            binding.etPassword.requestFocus()
            return false
        }
        
        return true
    }
    
    /**
     * Navigate to a fragment
     */
    private fun navigateToFragment(fragment: Fragment, tag: String) {
        Log.d(TAG, "navigateToFragment 호출됨: $tag")
        
        try {
            // 현재 표시된 프래그먼트 확인
            val currentFragment = supportFragmentManager.findFragmentById(R.id.login_container)
            if (currentFragment != null && currentFragment.javaClass.simpleName == fragment.javaClass.simpleName) {
                Log.d(TAG, "이미 같은 프래그먼트가 표시되어 있습니다: $tag")
                return
            }
            
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                .replace(R.id.login_container, fragment, tag)
                .addToBackStack(tag)
                .commit()
            
            // 로그 추가
            Log.d(TAG, "Navigate to fragment: $tag, BackStack Count: ${supportFragmentManager.backStackEntryCount + 1}")
            
            // 백 스택 상태 로깅
            supportFragmentManager.executePendingTransactions()
            logBackStackState()
        } catch (e: Exception) {
            Log.e(TAG, "프래그먼트 전환 중 오류 발생: $tag", e)
        }
    }
    
    /**
     * Navigate to Find ID fragment
     */
    private fun navigateToFindIdFragment() {
        val findIdFragment = FindIdFragment.newInstance()
        showFragmentContainer()
        navigateToFragment(findIdFragment, FindIdFragment::class.java.simpleName)
    }
    
    /**
     * Navigate to Find Password fragment
     */
    private fun navigateToFindPasswordFragment() {
        val findPasswordFragment = FindPasswordFragment.newInstance()
        showFragmentContainer()
        navigateToFragment(findPasswordFragment, FindPasswordFragment::class.java.simpleName)
    }
    
    /**
     * Navigate to Signup fragment
     */
    private fun navigateToSignupFragment() {
        val signupFragment = SignupFragment.newInstance()
        showFragmentContainer()
        navigateToFragment(signupFragment, SignupFragment::class.java.simpleName)
    }
    
    /**
     * Navigate to Signup Profile fragment - 회원가입 프로필 화면으로 이동
     */
    fun navigateToProfileFragment(fragment: Fragment) {
        showFragmentContainer()
        navigateToFragment(fragment, fragment.javaClass.simpleName)
    }

    /**
     * Set loading state
     */
    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.btnGoogleLogin.isEnabled = !isLoading
    }
    
    /**
     * Pops back a fragment from back stack and shows login UI if stack is empty
     * @return true if back stack was popped, false otherwise
     */
    fun popBackStackOrShowLoginUI(): Boolean {
        Log.d(TAG, "popBackStackOrShowLoginUI 호출됨: 백스택 수: ${supportFragmentManager.backStackEntryCount}")
        logBackStackState()
        
        if (supportFragmentManager.backStackEntryCount > 0) {
            try {
                supportFragmentManager.popBackStackImmediate()
                
                // 백스택에 프래그먼트가 없으면 로그인 화면 보이기
                if (supportFragmentManager.backStackEntryCount == 0) {
                    showLoginUI()
                } else {
                    Log.d(TAG, "백스택 팝 후 남은 백스택 수: ${supportFragmentManager.backStackEntryCount}")
                    logBackStackState()
                }
                return true
            } catch (e: Exception) {
                Log.e(TAG, "백스택 팝 중 오류 발생", e)
                showLoginUI()
                return false
            }
        } else {
            showLoginUI()
            return false
        }
    }

    // Companion object with constants
    companion object {
        fun newInstance(): LoginActivity = LoginActivity()
        
        // Google Sign-In related constants
        private const val GOOGLE_PLAY_SERVICES_REQUEST_CODE = 9000
        private const val ERROR_NETWORK = 7
        private const val ERROR_CANCELED = 12501
    }
    
    /**
     * 화면 전환 및 백 스택 확인을 위한 디버깅 메서드
     * 현재 백 스택 상태를 로그로 출력
     */
    private fun logBackStackState() {
        val backStackCount = supportFragmentManager.backStackEntryCount
        Log.d(TAG, "현재 백 스택 개수: $backStackCount")
        
        for (i in 0 until backStackCount) {
            val entry = supportFragmentManager.getBackStackEntryAt(i)
            Log.d(TAG, "백 스택 [$i]: ${entry.name}")
        }
    }

    /**
     * ID 토큰 검증 및 디버깅을 위한 로그 출력
     * 개발/디버깅 목적으로만 사용
     */
    private suspend fun validateGoogleToken(idToken: String) = withContext(Dispatchers.IO) {
        try {
            // 토큰 검증 URL
            val url = URL("https://oauth2.googleapis.com/tokeninfo?id_token=$idToken")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            
            val responseCode = connection.responseCode
            Log.d(TAG, "Google 토큰 검증 응답 코드: $responseCode")
            
            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d(TAG, "토큰 검증 성공: $response")
                
                // 간단한 JSON 파싱
                val jsonObject = JSONObject(response)
                val email = jsonObject.optString("email", "")
                val issuer = jsonObject.optString("iss", "")
                val audience = jsonObject.optString("aud", "")
                val expiresAt = jsonObject.optLong("exp", 0)
                
                Log.d(TAG, "토큰 정보 - 이메일: $email")
                Log.d(TAG, "토큰 정보 - 발급자: $issuer")
                Log.d(TAG, "토큰 정보 - 대상자: $audience")
                
                // 토큰 만료 시간 계산
                val expiresAtDate = java.util.Date(expiresAt * 1000)
                val now = java.util.Date()
                val isExpired = now.after(expiresAtDate)
                
                Log.d(TAG, "토큰 만료 여부: $isExpired (만료 시간: $expiresAtDate)")
                
                return@withContext !isExpired
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                Log.e(TAG, "토큰 검증 실패: $errorResponse")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "토큰 검증 중 오류 발생", e)
            return@withContext false
        }
    }
}