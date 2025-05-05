package com.example.maite

import android.content.Context
import com.example.maite.AuthInterceptor
import com.example.maite.PreferencesUtil
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.logging.HttpLoggingInterceptor

object ApiClient {
    private const val BASE_URL = "http://3.39.205.32:8080/"

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
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor(AuthInterceptor(preferencesUtil))  // 인증 인터셉터 추가
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