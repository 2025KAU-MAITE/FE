package com.example.maite

import android.content.Context // Context import 추가
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// object 대신 class로 변경하거나, Context를 받는 getInstance 함수 제공
object MaiteRetrofitClient {
    private const val BASE_URL = "http://3.39.205.32:8080/"
    private var apiService: MaiteApiService? = null
    private var okHttpClient: OkHttpClient? = null // OkHttpClient 캐싱

    // Context를 받아 ApiService 인스턴스를 반환하는 함수
    fun getInstance(context: Context): MaiteApiService {
        if (apiService == null) {
            synchronized(this) { // 스레드 안전성 확보
                if (apiService == null) {
                    val appContext = context.applicationContext // Application Context 사용
                    val preferencesUtil = PreferencesUtil(appContext)
                    val authInterceptor = AuthInterceptor(preferencesUtil)

                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }

                    // OkHttpClient 빌더에 AuthInterceptor 추가
                    okHttpClient = OkHttpClient.Builder()
                        .addInterceptor(loggingInterceptor)
                        .addInterceptor(authInterceptor) // 인증 인터셉터 추가
                        .connectTimeout(60, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build()

                    val retrofit = Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(okHttpClient!!) // 생성된 OkHttpClient 사용
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                    apiService = retrofit.create(MaiteApiService::class.java)
                }
            }
        }
        return apiService!!
    }
}