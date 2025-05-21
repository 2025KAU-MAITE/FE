package com.example.maite.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.maite.PreferencesUtil
import com.example.maite.model.GoogleLoginResponse
import com.example.maite.model.GoogleLoginResult
import com.example.maite.model.LoginResponse
import com.example.maite.model.SocialSignupResponse
import com.example.maite.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "LoginViewModel"
    private val authRepository = AuthRepository(application)
    private val preferencesUtil = PreferencesUtil(application)

    private val _loginResult = MutableLiveData<LoginResponse>()
    val loginResult: LiveData<LoginResponse> = _loginResult

    private val _googleLoginResult = MutableLiveData<GoogleLoginResponse>()
    val googleLoginResult: LiveData<GoogleLoginResponse> = _googleLoginResult
    
    private val _socialSignupResult = MutableLiveData<SocialSignupResponse>()
    val socialSignupResult: LiveData<SocialSignupResponse> = _socialSignupResult

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    // 일반 로그인
    fun login(email: String, password: String) {
        _loading.value = true
        viewModelScope.launch {
            try {
                val response = authRepository.login(email, password)
                _loginResult.value = response
                
                if (response.isSuccess) {
                    preferencesUtil.saveAccessToken(response.result.accessToken)
                    
                    // 사용자 정보 불러오기
                    try {
                        val userInfo = authRepository.getUserInfo(response.result.accessToken)
                        if (userInfo.isSuccess) {
                            preferencesUtil.saveUserInfo(
                                userInfo.result.userId,
                                userInfo.result.name,
                                userInfo.result.email
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "사용자 정보 조회 실패", e)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "로그인 처리 중 오류가 발생했습니다: ${e.message}"
                Log.e(TAG, "로그인 오류", e)
            } finally {
                _loading.value = false
            }
        }
    }

    // Google 로그인
    fun googleLogin(idToken: String, name: String = "", accessToken: String = "") {
        _loading.value = true
        viewModelScope.launch {
            try {
                // 소셜 계정 이름 로깅
                if (name.isNotEmpty()) {
                    Log.d(TAG, "Google 로그인 - 이름 정보: $name")
                }
                
                // 서버에 Google ID 토큰 검증 요청 (accessToken 추가)
                val response = authRepository.googleLogin(idToken, accessToken)
                
                // 서버 응답에 따른 처리:
                // HTTP 200 (isSuccess = true): 로그인 성공
                // HTTP 500 (isSuccess = false): 미등록 사용자
                val isRegistered = response.isSuccess  // 200이면 true, 500이면 false
                
                // 응답 저장 (LoginResponse)
                _loginResult.value = response
                
                var email: String? = null
                var userName: String? = name.ifEmpty { null }  // 구글에서 받은 이름이 있으면 사용
                
                if (isRegistered) {
                    // 로그인 성공 케이스 (HTTP 200)
                    Log.d(TAG, "Google 로그인 성공: HTTP 200")
                    
                    // 토큰 저장
                    preferencesUtil.saveAccessToken(response.result.accessToken)
                    
                    // 사용자 정보 불러오기 시도
                    try {
                        val userInfo = authRepository.getUserInfo(response.result.accessToken)
                        if (userInfo.isSuccess) {
                            email = userInfo.result.email
                            userName = userInfo.result.name
                            
                            preferencesUtil.saveUserInfo(
                                userInfo.result.userId,
                                userInfo.result.name,
                                userInfo.result.email
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Google 사용자 정보 조회 실패", e)
                    }
                } else {
                    // 미등록 사용자 케이스 (HTTP 500)
                    Log.d(TAG, "미등록 Google 사용자: HTTP 500")
                }
                
                // GoogleLoginResponse 생성하여 ViewModel에서 별도 관리
                val googleResponse = GoogleLoginResponse(
                    isSuccess = response.isSuccess,
                    code = response.code,
                    message = response.message,
                    result = GoogleLoginResult(
                        accessToken = response.result.accessToken,
                        message = response.result.message
                    )
                )
                
                // 최종 처리된 Google 로그인 결과 설정
                _googleLoginResult.value = googleResponse
                
            } catch (e: Exception) {
                _errorMessage.value = "Google 로그인 처리 중 오류가 발생했습니다: ${e.message}"
                Log.e(TAG, "Google 로그인 오류", e)
            } finally {
                _loading.value = false
            }
        }
    }
    
    // 소셜 로그인 회원가입 완료
    fun completeSocialSignup(email: String, name: String, provider: String, phoneNumber: String, address: String) {
        _loading.value = true
        viewModelScope.launch {
            try {
                Log.d(TAG, "소셜 회원가입 시작: email=$email, name=$name, provider=$provider")
                
                // 필수 항목 검증
                if (email.isBlank() || name.isBlank() || phoneNumber.isBlank() || address.isBlank()) {
                    _errorMessage.value = "모든 정보를 입력해주세요."
                    _loading.value = false
                    return@launch
                }
                
                val response = authRepository.completeSocialSignup(
                    email = email,
                    name = name,
                    provider = provider,
                    phoneNumber = phoneNumber,
                    address = address
                )
                
                Log.d(TAG, "소셜 회원가입 응답: isSuccess=${response.isSuccess}")
                _socialSignupResult.value = response
                
                if (response.isSuccess) {
                    // 액세스 토큰과 사용자 정보 저장
                    response.result.accessToken?.let { token ->
                        Log.d(TAG, "소셜 회원가입 성공: accessToken=${token.take(10)}...")
                        preferencesUtil.saveAccessToken(token)
                    } ?: Log.d(TAG, "소셜 회원가입 성공: accessToken=null")
                    
                    preferencesUtil.saveUserInfo(
                        response.result.userId,
                        response.result.name,
                        response.result.email
                    )
                } else {
                    Log.e(TAG, "소셜 회원가입 실패: ${response.message}")
                    
                    // 이미 존재하는 이메일인 경우 특별 오류 메시지 설정
                    if (response.message.contains("이미 존재하는 이메일")) {
                        _errorMessage.value = "이미 가입된 이메일입니다. 일반 로그인을 시도해보세요."
                    } else {
                        _errorMessage.value = "소셜 회원가입 실패: ${response.message}"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "소셜 회원가입 처리 중 오류 발생", e)
                _errorMessage.value = "소셜 회원가입 처리 중 오류가 발생했습니다: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Check if a user is registered with Google ID token
     * 서버에서 HTTP 200이면 로그인 성공, 500이면 미등록 사용자
     */
    fun checkGoogleRegistration(idToken: String, accessToken: String = "") {
        _loading.value = true
        viewModelScope.launch {
            try {
                // 서버에 Google ID 토큰으로 사용자 조회 요청 (accessToken 추가)
                val response = authRepository.googleLogin(idToken, accessToken)
                
                // 서버 응답에 따라 처리
                // HTTP 200 (isSuccess = true): 로그인 성공, 등록된 사용자
                // HTTP 500 (isSuccess = false): 미등록 사용자
                val isRegistered = response.isSuccess
                
                var email: String? = null
                var name: String? = null
                
                if (isRegistered) {
                    // 등록된 사용자인 경우 사용자 정보 조회
                    try {
                        val userInfo = authRepository.getUserInfo(response.result.accessToken)
                        if (userInfo.isSuccess) {
                            email = userInfo.result.email
                            name = userInfo.result.name
                            
                            preferencesUtil.saveUserInfo(
                                userInfo.result.userId,
                                userInfo.result.name,
                                userInfo.result.email
                            )
                            preferencesUtil.saveAccessToken(response.result.accessToken)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "사용자 정보 조회 실패", e)
                    }
                }
                
                // 로그인 결과 저장 (LoginResponse)
                _loginResult.value = response
                
                // GoogleLoginResponse 생성
                val googleResponse = GoogleLoginResponse(
                    isSuccess = response.isSuccess,
                    code = response.code,
                    message = response.message,
                    result = GoogleLoginResult(
                        accessToken = response.result.accessToken,
                        message = response.result.message
                    )
                )
                
                _googleLoginResult.value = googleResponse
                
            } catch (e: Exception) {
                _errorMessage.value = "구글 계정 확인 중 오류가 발생했습니다: ${e.message}"
                Log.e(TAG, "구글 계정 확인 오류", e)
            } finally {
                _loading.value = false
            }
        }
    }
}