package com.example.maite.model

import com.example.maite.model.EmailCheckRequest
import com.example.maite.model.EmailCheckResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    // ✅ 이메일 중복 확인 API (POST /auth/signup/check)
    @POST("auth/signup/check")
    suspend fun checkEmailDuplicate(@Body request: EmailCheckRequest): EmailCheckResponse
}