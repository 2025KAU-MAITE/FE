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
     * 이메일 중복 확인 (로컬 구현)
     */
    suspend fun checkEmailDuplicate(email: String): EmailCheckResponse {
        return withContext(Dispatchers.IO) {
            // 네트워크 요청 시뮬레이션
            delay(1000)
            
            Log.d(TAG, "로컬 이메일 중복 확인: $email")
            
            // 테스트용 이메일 목록에 있는지 확인
            val isDuplicated = usedEmails.contains(email)
            val message = if (isDuplicated) {
                "$email 이미 사용 중인 이메일입니다."
            } else {
                "$email 사용 가능한 이메일입니다."
            }
            
            // 응답 생성
            EmailCheckResponse(
                isSuccess = true,
                code = "COMMON200",
                message = "성공입니다.",
                result = EmailCheckResult(
                    message = message,
                    duplicated = isDuplicated
                )
            )
            
            // 실제 API 구현은 주석 처리
            // authApi.checkEmailDuplicate(email)
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