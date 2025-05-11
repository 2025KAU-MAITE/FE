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
    fun googleLogin(idToken: String) {
        _loading.value = true
        viewModelScope.launch {
            try {
                // 서버에 Google ID 토큰 검증 요청
                val response = authRepository.googleLogin(idToken)
                
                // 서버 응답으로부터 isRegistered 플래그 가정
                // 실제로는 서버 API 응답에 포함되어야 함 (여기서는 예시로 처리)
                val isRegistered = true // 기본적으로 이미 가입한 사용자로 처리
                
                // GoogleLoginResponse 생성하여 ViewModel에서 관리
                val googleResponse = GoogleLoginResponse(
                    isSuccess = response.isSuccess,
                    code = response.code,
                    message = response.message,
                    result = GoogleLoginResult(
                        accessToken = response.result.accessToken,
                        idToken = idToken,  // 원본 idToken 저장
                        message = response.result.message,
                        isRegistered = isRegistered,
                        email = null, // 사용자 정보 조회로 채울 수 있음
                        name = null   // 사용자 정보 조회로 채울 수 있음
                    )
                )
                
                _googleLoginResult.value = googleResponse
                
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
                            
                            // Google 로그인 결과에 사용자 정보 추가
                            googleResponse.result.let { result ->
                                val updatedResult = GoogleLoginResult(
                                    accessToken = result.accessToken,
                                    idToken = result.idToken,
                                    message = result.message,
                                    isRegistered = result.isRegistered,
                                    email = userInfo.result.email,
                                    name = userInfo.result.name
                                )
                                
                                _googleLoginResult.value = GoogleLoginResponse(
                                    isSuccess = googleResponse.isSuccess,
                                    code = googleResponse.code,
                                    message = googleResponse.message,
                                    result = updatedResult
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Google 사용자 정보 조회 실패", e)
                    }
                }
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
                    Log.d(TAG, "소셜 회원가입 성공: accessToken=${response.result.accessToken.take(10)}...")
                    preferencesUtil.saveAccessToken(response.result.accessToken)
                    preferencesUtil.saveUserInfo(
                        response.result.userId,
                        response.result.name,
                        response.result.email
                    )
                } else {
                    Log.e(TAG, "소셜 회원가입 실패: ${response.message}")
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
     * This method uses a server API which should indicate whether the user is already registered
     * If successful, it returns the GoogleLoginResponse object that includes user info
     */
    fun checkGoogleRegistration(idToken: String) {
        _loading.value = true
        viewModelScope.launch {
            try {
                // 서버에 Google ID 토큰으로 사용자 조회 요청
                val response = authRepository.googleLogin(idToken)
                
                // 서버 응답에 따라 처리
                // 여기서 서버는 200 OK 응답을 통해 사용자가 등록되어 있는지 여부와 함께
                // 사용자 정보를 반환하거나, 404 Not Found 등으로 미등록 사용자임을 알려야 함
                
                // GoogleLoginResult 생성
                val isRegistered = response.isSuccess
                
                // 사용자 정보 조회 시도
                var email: String? = null
                var name: String? = null
                
                if (response.isSuccess) {
                    try {
                        val userInfo = authRepository.getUserInfo(response.result.accessToken)
                        if (userInfo.isSuccess) {
                            email = userInfo.result.email
                            name = userInfo.result.name
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "사용자 정보 조회 실패", e)
                    }
                }
                
                // GoogleLoginResponse 생성
                val googleResponse = GoogleLoginResponse(
                    isSuccess = response.isSuccess,
                    code = response.code,
                    message = response.message,
                    result = GoogleLoginResult(
                        accessToken = response.result.accessToken,
                        idToken = idToken,
                        message = response.result.message,
                        isRegistered = isRegistered,
                        email = email,
                        name = name
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