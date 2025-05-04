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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.util.Log

class AuthRepository {
    private val TAG = "AuthRepository"
    private val authApi = ApiClient.retrofit.create(AuthApi::class.java)
    
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
                val request = SmsAuthSendRequest(phoneNumber = phoneNumber)
                
                // 실제 API 호출
                val response = authApi.sendSmsAuth(request)
                
                Log.d(TAG, "API 응답: isSuccess=${response.isSuccess}, message=${response.message}")
                
                response
            } catch (e: Exception) {
                Log.e(TAG, "SMS 인증번호 발송 API 오류: ${e.message}", e)
                
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
                    phoneNumber = phoneNumber,
                    verificationCode = verificationCode
                )
                
                // 실제 API 호출
                val response = authApi.verifySmsAuth(request)
                
                Log.d(TAG, "API 응답: isSuccess=${response.isSuccess}, message=${response.message}")
                if (response.result != null) {
                    Log.d(TAG, "결과: message=${response.result.message}")
                }
                
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
                if (response.result != null) {
                    Log.d(TAG, "회원가입 결과: userId=${response.result.userId}, email=${response.result.email}, registered=${response.result.registered}")
                }
                
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
}