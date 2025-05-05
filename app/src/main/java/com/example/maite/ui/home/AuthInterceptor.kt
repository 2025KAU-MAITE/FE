package com.example.maite.ui.home

import android.util.Log
import com.example.maite.PreferencesUtil
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 인증 토큰을 요청에 추가하는 인터셉터
 */
class AuthInterceptor(private val preferencesUtil: PreferencesUtil) : Interceptor {
    
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = preferencesUtil.getAccessToken()
        Log.d("AuthInterceptor", "Access token: $accessToken")
        
        val original = chain.request()
        
        // 토큰이 있는 경우에만 Authorization 헤더 추가
        val requestBuilder = if (!accessToken.isNullOrEmpty()) {
            original.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .method(original.method, original.body)
        } else {
            // 토큰이 없는 경우 원래 요청 사용
            Log.w("AuthInterceptor", "No access token found")
            original.newBuilder()
                .method(original.method, original.body)
        }
        
        val request = requestBuilder.build()
        return chain.proceed(request)
    }
}
