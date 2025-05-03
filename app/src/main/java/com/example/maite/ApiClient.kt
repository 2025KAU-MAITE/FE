package com.example.maite

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://3.39.205.32:8080/"

    // OkHttpClient 설정
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // 연결 타임아웃 설정
        .readTimeout(30, TimeUnit.SECONDS)     // 읽기 타임아웃 설정
        .writeTimeout(30, TimeUnit.SECONDS)    // 쓰기 타임아웃 설정
        .followRedirects(true)                 // 리다이렉션 허용
        .followSslRedirects(true)              // SSL 리다이렉션 허용
        .retryOnConnectionFailure(true)        // 연결 실패 시 재시도
        .build()

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)              // 커스텀 OkHttpClient 설정
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
