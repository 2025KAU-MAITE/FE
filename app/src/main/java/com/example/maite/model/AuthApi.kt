package com.example.maite.model

import com.example.maite.model.EmailCheckResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface AuthApi {

    // ✅ 이메일 중복 확인 API (GET /auth/signup/check)
    @GET("auth/signup/check")
    suspend fun checkEmailDuplicate(@Query("email") email: String): EmailCheckResponse
}