package com.example.maite.repository

import com.example.maite.ApiClient
import com.example.maite.model.AuthApi
import com.example.maite.model.EmailCheckResponse
import com.example.maite.model.EmailCheckResult
import com.example.maite.model.SignupRequest
import com.example.maite.model.SignupResponse
import com.example.maite.model.SignupResult
import com.example.maite.model.SmsAuthResponse
import com.example.maite.model.SmsAuthResult
import com.example.maite.model.SmsAuthSendRequest
import com.example.maite.model.SmsAuthVerifyRequest
import com.example.maite.model.LoginRequest
import com.example.maite.model.LoginResponse
import com.example.maite.model.LoginResult
import com.example.maite.model.GoogleLoginRequest
import com.example.maite.model.GoogleLoginResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.util.Log
import android.content.Context
import com.example.maite.UserInfoResponse
import com.example.maite.UserInfoResult
import com.example.maite.model.SocialSignupRequest
import com.example.maite.model.SocialSignupResponse
import com.example.maite.model.SocialSignupResult

class AuthRepository(private val context: Context) {
    private val TAG = "AuthRepository"
    private val authApi = ApiClient.getClient(context).create(AuthApi::class.java)
    
    // 테스트용 이메일 목록 (사용 중인 이메일로 간주)
    private val usedEmails = listOf("test@example.com", "used@gmail.com", "taken@naver.com")
    
    // 테스트용 인증번호
    private val testVerificationCode = "123456"
    
    /**
     * 이메일 중복 확인 (서버 API 연동)
     */
    suspend fun checkEmailDuplicate(email: String): EmailCheckResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "서버 이메일 중복 확인 API 호출: $email")
                
                // 실제 API 호출
                val response = authApi.checkEmailDuplicate(email)
                
                Log.d(TAG, "API 응답: isSuccess=${response.isSuccess}, message=${response.message}, duplicated=${response.result.duplicated}")
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "이메일 중복 확인 API 오류: ${e.message}", e)
                
                // API 호출 실패 시 오류 응답 생성
                EmailCheckResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = EmailCheckResult(
                        duplicated = false,
                        message = "이메일 중복 확인 중 오류가 발생했습니다"
                    )
                )
            }
        }
    }
    
    /**
     * SMS 인증번호 발송 (서버 API 연동)
     */
    suspend fun sendSmsAuth(phoneNumber: String): SmsAuthResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "서버 SMS 인증번호 발송 API 호출: $phoneNumber")
                
                // POST 요청에 필요한 데이터 생성
                val request = SmsAuthSendRequest(phonenumber = phoneNumber)
                Log.d(TAG, "SMS 인증 요청 데이터: $request")
                
                // 실제 API 호출 전 로그
                Log.d(TAG, "API 호출 시작: auth/signup/send-code 엔드포인트로 요청 전송")
                
                // 실제 API 호출
                val response = authApi.sendSmsAuth(request)
                
                // API 응답 상세 로깅
                Log.d(TAG, "API 응답 받음: isSuccess=${response.isSuccess}, code=${response.code}, message=${response.message}")
                Log.d(TAG, "응답 결과 상세: ${response.result}")
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "SMS 인증번호 발송 API 오류: ${e.message}", e)
                Log.e(TAG, "상세 예외 정보: ${e.javaClass.name}")
                if (e.cause != null) {
                    Log.e(TAG, "원인 예외: ${e.cause?.message}", e.cause)
                }
                
                // API 호출 실패 시 오류 응답 생성
                SmsAuthResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = SmsAuthResult(
                        message = "SMS 인증번호 발송 중 오류가 발생했습니다"
                    )
                )
            }
        }
    }
    
    /**
     * SMS 인증번호 확인 (서버 API 연동)
     */
    suspend fun verifySmsAuth(phoneNumber: String, verificationCode: String): SmsAuthResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "서버 SMS 인증번호 확인 API 호출: 전화번호=$phoneNumber, 인증번호=$verificationCode")
                
                // POST 요청에 필요한 데이터 생성
                val request = SmsAuthVerifyRequest(
                    phonenumber = phoneNumber,
                    verificationCode = verificationCode
                )
                
                // 실제 API 호출
                val response = authApi.verifySmsAuth(request)
                
                Log.d(TAG, "API 응답: isSuccess=${response.isSuccess}, message=${response.message}")
                Log.d(TAG, "결과: message=${response.result.message}")
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "SMS 인증번호 확인 API 오류: ${e.message}", e)
                
                // API 호출 실패 시 오류 응답 생성
                SmsAuthResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = SmsAuthResult(
                        message = "SMS 인증번호 확인 중 오류가 발생했습니다"
                    )
                )
            }
        }
    }

    /**
     * 회원가입 처리 (서버 API 연동)
     */
    suspend fun signup(email: String, password: String, name: String, phoneNumber: String, address: String): SignupResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "회원가입 API 호출: 이메일=$email, 이름=$name, 전화번호=$phoneNumber, 주소=$address")
                
                // POST 요청에 필요한 데이터 생성
                val request = SignupRequest(
                    email = email,
                    password = password,
                    name = name,
                    phoneNumber = phoneNumber,
                    address = address
                )
                
                // 실제 API 호출
                val response = authApi.signup(request)
                
                Log.d(TAG, "API 응답: isSuccess=${response.isSuccess}, message=${response.message}")
                Log.d(TAG, "회원가입 결과: userId=${response.result.userId}, email=${response.result.email}, registered=${response.result.registered}")
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "회원가입 API 오류: ${e.message}", e)
                
                // API 호출 실패 시 오류 응답 생성
                SignupResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = SignupResult(
                        userId = 0,
                        email = email,
                        name = name,
                        registeredAt = "",
                        message = "회원가입 처리 중 오류가 발생했습니다",
                        registered = false
                    )
                )
            }
        }
    }

    /**
     * 로그인 처리 (서버 API 연동)
     */
    suspend fun login(email: String, password: String): LoginResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "로그인 API 호출: 이메일=$email")
                
                // POST 요청에 필요한 데이터 생성
                val request = LoginRequest(
                    email = email,
                    password = password
                )
                
                // 실제 API 호출
                val response = authApi.login(request)
                
                Log.d(TAG, "API 응답: isSuccess=${response.isSuccess}, message=${response.message}")
                Log.d(TAG, "로그인 결과: accessToken=${response.result.accessToken.take(10)}...")
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "로그인 API 오류: ${e.message}", e)
                
                // API 호출 실패 시 오류 응답 생성
                LoginResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = LoginResult(
                        accessToken = "",
                        message = "로그인 처리 중 오류가 발생했습니다"
                    )
                )
            }
        }
    }

    /**
     * Google 로그인 처리 (서버 API 연동)
     */
    suspend fun googleLogin(idToken: String): LoginResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Google 로그인 API 호출: idToken 길이=${idToken.length}")
                
                // POST 요청에 필요한 데이터 생성
                val request = GoogleLoginRequest(
                    idToken = idToken
                )
                
                // 실제 API 호출
                val response = authApi.googleLogin(request)
                
                Log.d(TAG, "API 응답: isSuccess=${response.isSuccess}, message=${response.message}")
                Log.d(TAG, "로그인 결과: accessToken=${response.result.accessToken.take(10)}...")
                
                // GoogleLoginResponse를 LoginResponse로 변환
                // GoogleLoginResult는 LoginResult와 다른 구조를 가지므로 필요한 값만 추출
                LoginResponse(
                    isSuccess = response.isSuccess,
                    code = response.code,
                    message = response.message,
                    result = LoginResult(
                        accessToken = response.result.accessToken,
                        message = response.result.message
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Google 로그인 API 오류: ${e.message}", e)
                
                // API 호출 실패 시 오류 응답 생성
                LoginResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = LoginResult(
                        accessToken = "",
                        message = "Google 로그인 처리 중 오류가 발생했습니다"
                    )
                )
            }
        }
    }

    suspend fun getUserInfo(token: String): UserInfoResponse {
        return withContext(Dispatchers.IO) {
            try {
                authApi.getUserInfo("Bearer $token")
            } catch (e: Exception) {
                UserInfoResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "사용자 정보 조회 실패: ${e.message}",
                    result = UserInfoResult(
                        userId = 0L,
                        email = "",
                        name = ""
                    )
                )
            }
        }
    }

    /**
     * 소셜 로그인 회원가입 처리 (서버 API 연동)
     */
    suspend fun completeSocialSignup(
        email: String,
        name: String,
        provider: String,
        phoneNumber: String,
        address: String
    ): SocialSignupResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "소셜 로그인 회원가입 API 호출: 이메일=$email, 이름=$name, 제공자=$provider")
                
                // POST 요청에 필요한 데이터 생성
                val request = SocialSignupRequest(
                    email = email,
                    name = name,
                    provider = provider,
                    phonenumber = phoneNumber, // API 요구사항에 따라 phonenumber 사용
                    address = address
                )
                
                // 실제 API 호출
                val response = authApi.completeSocialSignup(request)
                
                Log.d(TAG, "API 응답: isSuccess=${response.isSuccess}, message=${response.message}")
                Log.d(TAG, "회원가입 결과: userId=${response.result.userId}, email=${response.result.email}, registered=${response.result.registered}")
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "소셜 로그인 회원가입 API 오류: ${e.message}", e)
                
                // API 호출 실패 시 오류 응답 생성
                SocialSignupResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "서버 연결 오류: ${e.message}",
                    result = SocialSignupResult(
                        userId = 0,
                        email = email,
                        name = name,
                        registeredAt = "",
                        message = "소셜 로그인 회원가입 처리 중 오류가 발생했습니다",
                        registered = false,
                        accessToken = "",
                        idToken = ""
                    )
                )
            }
        }
    }
}