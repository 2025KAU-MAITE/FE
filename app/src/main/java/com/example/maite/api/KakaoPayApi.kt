package com.example.maite.api

import com.example.maite.model.KakaoPayReadyRequest
import com.example.maite.model.KakaoPayReadyResponse
import com.example.maite.model.KakaoPaySuccessRequest
import com.example.maite.model.KakaoPaySuccessResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 카카오페이 결제 API 인터페이스
 */
interface KakaoPayApi {
    
    /**
     * 카카오페이 결제 준비 API
     * POST /kakao/ready
     */
    @POST("kakao/ready")
    suspend fun readyPayment(@Body request: KakaoPayReadyRequest): Response<KakaoPayReadyResponse>
    
    /**
     * 카카오페이 결제 성공 처리 API
     * POST /kakao/success
     */
    @POST("kakao/success")
    suspend fun processPaymentSuccess(@Body request: KakaoPaySuccessRequest): Response<KakaoPaySuccessResponse>
}
