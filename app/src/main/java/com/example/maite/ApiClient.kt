package com.example.maite

import android.content.Context
import com.example.maite.AuthInterceptor
import com.example.maite.PreferencesUtil
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.logging.HttpLoggingInterceptor
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Request

object ApiClient {
    private const val BASE_URL = "http://3.39.205.32:8080/"
    private const val TAG = "ApiClient"

    private var retrofit: Retrofit? = null

    fun getClient(context: Context): Retrofit {
        if (retrofit == null) {
            createNewInstance(context)
        }
        return retrofit!!
    }
    
    // 새로운 Retrofit 인스턴스를 생성하는 메서드
    private fun createNewInstance(context: Context) {
        val preferencesUtil = PreferencesUtil(context)

        // 로깅 인터셉터 추가 (디버깅 용도)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY  // 모든 요청/응답의 헤더와 본문을 로깅
        }

        // Google 로그인 API 요청을 위한 특별 인터셉터
        val googleApiInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            
            // Google 로그인 API 요청에만 적용
            if (original.url.encodedPath.contains("/auth/login-google")) {
                Log.d(TAG, "Google 로그인 API 요청 감지: 특수 헤더 추가")
                requestBuilder.addHeader("Content-Type", "application/json")
                requestBuilder.addHeader("Accept", "application/json")
                requestBuilder.addHeader("User-Agent", "MAITE-Android-App")
                
                // 요청 바디 로깅을 위한 처리
                val requestBody = original.body
                if (requestBody != null) {
                    try {
                        val buffer = okio.Buffer()
                        requestBody.writeTo(buffer)
                        val requestBodyString = buffer.readUtf8()
                        Log.d(TAG, "Google 로그인 API 요청 바디: $requestBodyString")
                    } catch (e: Exception) {
                        Log.e(TAG, "요청 바디 로깅 실패", e)
                    }
                }
            }
            
            // 요청 실행
            val response = chain.proceed(requestBuilder.build())
            
            // Google 로그인 API 응답 처리
            if (original.url.encodedPath.contains("/auth/login-google")) {
                val responseCode = response.code
                Log.d(TAG, "Google 로그인 API 응답 코드: $responseCode")
                
                if (responseCode != 200) {
                    val responseBody = response.peekBody(Long.MAX_VALUE).string()
                    Log.e(TAG, "Google 로그인 API 오류 응답: $responseBody")
                }
            }
            
            response
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor(AuthInterceptor(preferencesUtil))  // 인증 인터셉터 추가
            .addInterceptor(googleApiInterceptor)  // Google API 인터셉터 추가
            .addInterceptor(loggingInterceptor)  // 로깅 인터셉터 추가
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // Retrofit 인스턴스를 초기화하는 메서드 (로그인 오류 시 호출)
    fun resetClient(context: Context): Retrofit {
        retrofit = null
        return getClient(context)
    }
}