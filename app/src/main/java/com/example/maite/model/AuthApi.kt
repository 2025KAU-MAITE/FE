package com.example.maite.model

import com.example.maite.model.EmailCheckResponse
import com.example.maite.model.SmsAuthResponse
import com.example.maite.model.SmsAuthSendRequest
import com.example.maite.model.SmsAuthVerifyRequest
import com.example.maite.model.GoogleLoginRequest
import com.example.maite.model.GoogleLoginResponse
import com.example.maite.UserInfoResponse
import com.example.maite.model.ApiResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.Part

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
    
    // ✅ 아이디 찾기 - 인증번호 발송 API (POST /auth/find-id/send-code)
    @POST("auth/find-id/send-code")
    suspend fun sendFindIdSmsAuth(@Body request: FindIdSendRequest): SmsAuthResponse
    
    // ✅ 아이디 찾기 - 인증번호 확인 API (POST /auth/find-id/verify)
    @POST("auth/find-id/verify")
    suspend fun verifyFindId(@Body request: FindIdVerifyRequest): FindIdResponse
    
    // ✅ 비밀번호 재설정 - 인증번호 발송 API (POST /auth/reset-password/send-code)
    @POST("auth/reset-password/send-code")
    suspend fun sendResetPasswordCode(@Body request: ResetPasswordSendRequest): SmsAuthResponse
    
    // ✅ 비밀번호 재설정 - 인증번호 확인 API (POST /auth/reset-password/verify)
    @POST("auth/reset-password/verify")
    suspend fun verifyResetPasswordCode(@Body request: ResetPasswordVerifyRequest): ResetPasswordResponse
    
    // ✅ 비밀번호 재설정 - 비밀번호 업데이트 API (POST /auth/reset-password)
    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordUpdateRequest): ResetPasswordResponse
    
    // ✅ 회원가입 API (POST /auth/signup)
    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): SignupResponse
    
    // ✅ 로그인 API (POST /auth/login)
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    // ✅ Google 로그인 API (POST /auth/login-google)
    @POST("auth/login-google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): GoogleLoginResponse

    // ✅ 소셜 로그인 회원가입 API (POST /auth/complete-social-signup)
    @POST("auth/complete-social-signup")
    suspend fun completeSocialSignup(
        @Query("email") email: String,
        @Query("name") name: String,
        @Query("provider") provider: String,
        @Query("phonenumber") phonenumber: String,
        @Query("address") address: String,
        @Query("profileImageUrl") profileImageUrl: String? = null
    ): SocialSignupResponse

    @GET("auth/me")
    suspend fun getUserInfo(@Header("Authorization") token: String): UserInfoResponse

    // ✅ 로그아웃 API (POST /auth/logout)
    @POST("auth/logout")
    suspend fun logout(): ApiResponse<String>

    // ✅ 회원가입 프로필 이미지 업로드 API (POST /auth/signup/upload-image)
    @Multipart
    @POST("auth/signup/upload-image")
    suspend fun uploadSignupProfileImage(@Part file: MultipartBody.Part): Response<ApiResponse<String>>
}