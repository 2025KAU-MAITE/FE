package com.example.maite.repository

import com.example.maite.ApiClient
import com.example.maite.model.AuthApi
import com.example.maite.model.EmailCheckResponse
import com.example.maite.model.EmailCheckResult
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
                        message = "이메일 중복 확인 중 오류가 발생했습니다",
                        duplicated = false
                    )
                )
            }
        }
    }
    
    /**
     * SMS 인증번호 발송 (로컬 구현)
     */
    suspend fun sendSmsAuth(phoneNumber: String): SmsAuthResponse {
        return withContext(Dispatchers.IO) {
            // 네트워크 요청 시뮬레이션
            delay(1000)
            
            Log.d(TAG, "로컬 SMS 인증번호 발송: $phoneNumber -> $testVerificationCode")
            
            // 응답 생성
            SmsAuthResponse(
                isSuccess = true,
                code = "COMMON200",
                message = "성공입니다.",
                result = SmsAuthResult(
                    message = "인증번호가 발송되었습니다. (테스트 코드: $testVerificationCode)"
                )
            )
            
            // 실제 API 구현은 주석 처리
            // authApi.sendSmsAuth(SmsAuthSendRequest(phoneNumber))
        }
    }
    
    /**
     * SMS 인증번호 확인 (로컬 구현)
     */
    suspend fun verifySmsAuth(phoneNumber: String, verificationCode: String): SmsAuthResponse {
        return withContext(Dispatchers.IO) {
            // 네트워크 요청 시뮬레이션
            delay(1000)
            
            Log.d(TAG, "로컬 SMS 인증번호 확인: $phoneNumber, 입력코드: $verificationCode, 예상코드: $testVerificationCode")
            
            // 테스트용 인증번호와 비교
            val isVerified = verificationCode == testVerificationCode
            val message = if (isVerified) {
                "인증이 완료되었습니다."
            } else {
                "인증번호가 일치하지 않습니다."
            }
            
            // 응답 생성
            SmsAuthResponse(
                isSuccess = isVerified,
                code = if (isVerified) "COMMON200" else "AUTH001",
                message = if (isVerified) "성공입니다." else "인증 실패",
                result = SmsAuthResult(
                    message = message
                )
            )
            
            // 실제 API 구현은 주석 처리
            // authApi.verifySmsAuth(SmsAuthVerifyRequest(phoneNumber, verificationCode))
        }
    }
}