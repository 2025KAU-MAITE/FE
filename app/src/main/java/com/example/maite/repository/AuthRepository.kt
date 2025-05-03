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
                "$email은 이미 사용 중인 이메일입니다."
            } else {
                "$email은 사용 가능한 이메일입니다."
            }
            
            // 응답 생성