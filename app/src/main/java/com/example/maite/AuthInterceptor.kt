package com.example.maite

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val preferencesUtil: PreferencesUtil) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val urlPath = originalRequest.url.encodedPath
        
        // 디버깅을 위한 로그 추가
        Log.d("AuthInterceptor", "요청 경로: $urlPath")
        Log.d("AuthInterceptor", "요청 메서드: ${originalRequest.method}")
        
        // 인증이 필요없는 API 경로 목록
        val noAuthPaths = listOf(
            "/auth/login",
            "/auth/signup",
            "/auth/signup/check",
            "/auth/signup/send-code",
            "/auth/signup/verify-code",
            "/auth/find-id/send-code",   // 아이디 찾기 - 인증번호 발송
            "/auth/find-id/verify",      // 아이디 찾기 - 인증번호 확인
            "/auth/reset-password/send-code", // 비밀번호 찾기 - 인증번호 발송
            "/auth/reset-password/verify",    // 비밀번호 찾기 - 인증번호 확인
            "/auth/reset-password",           // 비밀번호 업데이트
            "/auth/complete-social-signup",   // 소셜 로그인 회원가입 - 추가됨
            "/auth/login-google"              // 구글 로그인 API 경로 추가
        )
        
        // 인증이 필요없는 API인 경우 토큰을 추가하지 않음
        if (noAuthPaths.any { urlPath.contains(it) }) {
            // 소셜 로그인 회원가입 경로인 경우 특별 로깅
            if (urlPath.contains("/auth/complete-social-signup")) {
                Log.d("AuthInterceptor", "소셜 로그인 회원가입 API 호출 감지됨")
                Log.d("AuthInterceptor", "인증 헤더 있는지 확인: ${originalRequest.header("Authorization") != null}")
                // 현재 Authorization 헤더가 있는 경우 로깅
                originalRequest.header("Authorization")?.let { 
                    Log.d("AuthInterceptor", "인증 헤더: ${it.take(15)}...")
                }
                
                // 추가 디버깅용 로그
                Log.d("AuthInterceptor", "요청 방식: ${originalRequest.method}")
                Log.d("AuthInterceptor", "Content-Type: ${originalRequest.header("Content-Type")}")
                originalRequest.body?.let {
                    Log.d("AuthInterceptor", "Request body 있음: Content-Length=${it.contentLength()}")
                } ?: Log.d("AuthInterceptor", "Request body 없음")
            }
            
            return chain.proceed(originalRequest)
        }
        
        val token = preferencesUtil.getAccessToken()
        
        return if (token != null) {
            val newRequest = originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}