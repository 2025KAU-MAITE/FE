package com.example.maite.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.maite.PreferencesUtil
import com.example.maite.model.GoogleLoginResponse
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
                val response = authRepository.googleLogin(idToken)
                
                // GoogleLoginResponse를 ViewModel에서 별도 관리
                // 이미 가입된 경우와 신규 사용자인 경우를 구분하기 위함
                val googleResponse = GoogleLoginResponse(
                    isSuccess = response.isSuccess,
                    code = response.code,
                    message = response.message,
                    result = GoogleLoginResult(
                        accessToken = response.result.accessToken,
                        idToken = idToken,  // 원본 idToken 저장
                        message = response.result.message,
                        isRegistered = true  // 기본값, 서버 응답에 따라 설정
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
                val response = authRepository.completeSocialSignup(
                    email = email,
                    name = name,
                    provider = provider,
                    phoneNumber = phoneNumber,
                    address = address
                )
                
                _socialSignupResult.value = response
                
                if (response.isSuccess) {
                    // 액세스 토큰과 사용자 정보 저장
                    preferencesUtil.saveAccessToken(response.result.accessToken)
                    preferencesUtil.saveUserInfo(
                        response.result.userId,
                        response.result.name,
                        response.result.email
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = "소셜 회원가입 처리 중 오류가 발생했습니다: ${e.message}"
                Log.e(TAG, "소셜 회원가입 오류", e)
            } finally {
                _loading.value = false
            }
        }
    }
}