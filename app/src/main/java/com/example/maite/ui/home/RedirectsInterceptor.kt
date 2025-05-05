package com.example.maite.ui.home

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 리다이렉션 인터셉터
 * OkHttp의 기본 리다이렉션 제한을 조정하기 위한 인터셉터
 */
class RedirectsInterceptor : Interceptor {
    companion object {
        private const val MAX_REDIRECTS = 5 // 최대 리다이렉션 횟수
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        
        var redirectCount = 0
        var redirectedResponse = response
        
        while (redirectCount < MAX_REDIRECTS && isRedirect(redirectedResponse.code)) {
            val location = redirectedResponse.header("Location") ?: break
            
            redirectedResponse.close()
            
            val newRequest = request.newBuilder()
                .url(location)
                .build()
                
            redirectedResponse = chain.proceed(newRequest)
            redirectCount++
        }
        
        return redirectedResponse
    }
    
    private fun isRedirect(code: Int): Boolean {
        return code == 301 || code == 302 || code == 303 || code == 307 || code == 308
    }
}
