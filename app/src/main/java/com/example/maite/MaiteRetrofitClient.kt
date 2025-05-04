package com.example.maite 

import android.util.Log
import com.example.maite.util.AuthTokenManager // AuthTokenManager import 확인
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object MaiteRetrofitClient {
    private const val BASE_URL = "http://3.39.205.32:8080/" // API 서버 주소
    private const val TAG = "AuthInterceptor" // 로그용 태그

    // HTTP 요청/응답 로깅 인터셉터
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // 로그 레벨 설정
    }

    // 인증 헤더 추가 인터셉터
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // AuthTokenManager를 통해 저장된 토큰 가져오기
        val token = AuthTokenManager.getToken()

        if (token == null) {
            // 토큰이 없는 경우 (로그인되지 않은 상태)
            Log.w(TAG, "인증 토큰 없음. API 요청 중단: ${originalRequest.url}")
            // 서버로 요청을 보내지 않고 즉시 401 Unauthorized 응답 반환
            Response.Builder()
                .code(401)
                .protocol(Protocol.HTTP_1_1)
                .message("로그인이 필요합니다.")
                .body("{\"error\":\"로그인이 필요합니다.\"}".toResponseBody(null)) // 간단한 JSON 에러 본문
                .request(originalRequest)
                .build()
        } else {
            // 토큰이 있는 경우: Authorization 헤더 추가
            requestBuilder.header("Authorization", "Bearer $token")
            val request = requestBuilder.build()
            // 수정된 요청으로 계속 진행
            chain.proceed(request)
        }
    }

    // OkHttpClient 설정 (인터셉터 추가)
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // 로깅 인터셉터 추가
        .addInterceptor(authInterceptor)   // 인증 인터셉터 추가
        .connectTimeout(60, TimeUnit.SECONDS) // 연결 타임아웃
        .readTimeout(60, TimeUnit.SECONDS)    // 읽기 타임아웃
        .writeTimeout(60, TimeUnit.SECONDS)   // 쓰기 타임아웃
        .build()

    // Retrofit 인스턴스 생성
    val instance: MaiteApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // 인증 인터셉터가 포함된 OkHttpClient 사용
            .addConverterFactory(GsonConverterFactory.create()) // Gson 변환기 사용
            .build()
        // MaiteApiService 인터페이스 구현체 생성
        retrofit.create(MaiteApiService::class.java)
    }
}