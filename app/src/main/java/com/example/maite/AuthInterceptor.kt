package com.example.maite

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val preferencesUtil: PreferencesUtil) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
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