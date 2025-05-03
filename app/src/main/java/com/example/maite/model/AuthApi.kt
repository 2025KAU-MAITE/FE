package com.example.maite.model

import com.example.maite.model.EmailCheckResponse
import com.example.maite.model.SmsAuthResponse
import com.example.maite.model.SmsAuthSendRequest
import com.example.maite.model.SmsAuthVerifyRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {

    // ✅ 이메일 중복 확인 API (GET /auth/signup/check)
    @GET("auth/signup/check")
    suspend fun checkEmailDuplicate(@Query("email") email: String): EmailCheckResponse
    
    // ✅ SMS 인증번호 발송 API (POST /api/signup/send-code)
    @POST("auth/signup/send-code")
    suspend fun sendSmsAuth(@Body request: SmsAuthSendRequest): SmsAuthResponse
    
    // ✅ SMS 인증번호 확인 API (POST /auth/signup/verify-code)
    @POST("auth/signup/verify-code")
    suspend fun verifySmsAuth(@Body request: SmsAuthVerifyRequest): SmsAuthResponse
}