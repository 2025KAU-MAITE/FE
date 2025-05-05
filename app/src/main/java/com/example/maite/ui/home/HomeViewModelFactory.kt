package com.example.maite.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.maite.PreferencesUtil
import com.example.maite.data.repository.ProposalRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.example.maite.api.ProposalApi

/**
 * HomeViewModel 생성을 위한 팩토리 클래스
 * 의존성이 있는 ViewModel을 생성할 때 사용
 */
class HomeViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            // API 서비스 생성
            val proposalApi = createProposalApi()
            
            // 레포지토리 생성
            val proposalRepository = ProposalRepository(proposalApi)
            
            // ViewModel 생성 및 반환
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(proposalRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
    
    private fun createProposalApi(): ProposalApi {
        // PreferencesUtil 초기화
        val preferencesUtil = PreferencesUtil(context)
        
        // 로깅 인터셉터 생성
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // 요청 및 응답 본문 모두 로깅
        }
        
        // 인증 인터셉터 생성 - 모든 요청에 토큰을 추가
        val authInterceptor = com.example.maite.AuthInterceptor(preferencesUtil)
        
        // 리디렉션 인터셉터 생성 - 리디렉션 오류 해결
        val redirectsInterceptor = RedirectsInterceptor()
        
        // OkHttpClient 생성
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // 인증 인터셉터 추가
            .addInterceptor(loggingInterceptor) // 로깅 추가
            .addInterceptor(redirectsInterceptor) // 리디렉션 인터셉터 추가
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        // Gson 설정 - lenient 모드 활성화로 JSON 파싱 오류 방지
        val gson = com.google.gson.GsonBuilder()
            .setLenient()
            .create()
            
        // Retrofit 인스턴스 생성
        val retrofit = Retrofit.Builder()
            .baseUrl("http://3.39.205.32:8080/") // 스웨거 서버 URL (/ 로 끝나야 함)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()
        
        // API 서비스 생성
        return retrofit.create(ProposalApi::class.java)
    }
}
