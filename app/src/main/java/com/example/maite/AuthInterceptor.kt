package com.example.maite

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val preferencesUtil: PreferencesUtil) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val urlPath = originalRequest.url.encodedPath
        
        // 인증이 필요없는 API 경로 목록
        val noAuthPaths = listOf(
            "/auth/login",
            "/auth/signup",
            "/auth/signup/check",
            "/auth/signup/send-code",
            "/auth/signup/verify-code",
            "/auth/find-id/send-code",   // 아이디 찾기 - 인증번호 발송
            "/auth/find-id/verify"       // 아이디 찾기 - 인증번호 확인
        )
        
        // 인증이 필요없는 API인 경우 토큰을 추가하지 않음
        if (noAuthPaths.any { urlPath.contains(it) }) {
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