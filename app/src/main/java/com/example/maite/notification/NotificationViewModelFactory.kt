package com.example.maite.notification

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.maite.PreferencesUtil
import com.example.maite.api.ProposalApi
import com.example.maite.data.repository.ProposalRepository
import com.example.maite.ui.home.RedirectsInterceptor
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class NotificationViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            // API 서비스 생성
            val notificationApi = createNotificationApi()
            val proposalApi = createProposalApi()
            
            // 레포지토리 생성
            val notificationRepository = NotificationRepository(notificationApi)
            val proposalRepository = ProposalRepository(proposalApi)
            
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(notificationRepository, proposalRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
    
    private fun createNotificationApi(): NotificationApiService {
        // PreferencesUtil 초기화
        val preferencesUtil = PreferencesUtil(context)
        
        // 로깅 인터셉터 생성
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        // 인증 인터셉터 생성
        val authInterceptor = com.example.maite.AuthInterceptor(preferencesUtil)
        
        // OkHttpClient 생성
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        // Gson 설정
        val gson = GsonBuilder()
            .setLenient()
            .create()
            
        // Retrofit 인스턴스 생성
        val retrofit = Retrofit.Builder()
            .baseUrl("http://3.39.205.32:8080/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
        
        return retrofit.create(NotificationApiService::class.java)
    }
    
    private fun createProposalApi(): ProposalApi {
        // PreferencesUtil 초기화
        val preferencesUtil = PreferencesUtil(context)
        
        // 로깅 인터셉터 생성
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        // 인증 인터셉터 생성
        val authInterceptor = com.example.maite.AuthInterceptor(preferencesUtil)
        
        // 리디렉션 인터셉터 생성
        val redirectsInterceptor = RedirectsInterceptor()
        
        // OkHttpClient 생성
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(redirectsInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        // Gson 설정
        val gson = GsonBuilder()
            .setLenient()
            .create()
            
        // Retrofit 인스턴스 생성
        val retrofit = Retrofit.Builder()
            .baseUrl("http://3.39.205.32:8080/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
        
        return retrofit.create(ProposalApi::class.java)
    }
}