package com.example.maite.repository

import android.content.Context
import android.util.Log
import com.example.maite.ApiClient
import com.example.maite.api.KakaoPayApi
import com.example.maite.model.AuthApi
import com.example.maite.model.KakaoPayReadyRequest
import com.example.maite.model.KakaoPayReadyResponse
import com.example.maite.model.KakaoPaySuccessRequest
import com.example.maite.model.KakaoPaySuccessResponse
import com.example.maite.UserInfoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 카카오페이 결제 관련 API 호출을 담당하는 Repository
 */
class PaymentRepository(private val context: Context) {
    
    private val TAG = "PaymentRepository"
    private val kakaoPayApi = ApiClient.getClient(context).create(KakaoPayApi::class.java)
    private val authApi = ApiClient.getClient(context).create(AuthApi::class.java)
    
    /**
     * 카카오페이 결제 준비 API 호출
     * @param totalAmount 총 결제 금액
     * @param itemName 상품명
     * @param quantity 수량
     * @return KakaoPayReadyResponse
     */
    suspend fun readyKakaoPayment(
        totalAmount: Int,
        itemName: String,
        quantity: Int = 1
    ): KakaoPayReadyResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "카카오페이 결제 준비 API 호출: totalAmount=$totalAmount, itemName=$itemName, quantity=$quantity")
                
                val request = KakaoPayReadyRequest(
                    totalAmount = totalAmount,
                    itemName = itemName,
                    quantity = quantity
                )
                
                val response = kakaoPayApi.readyPayment(request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d(TAG, "카카오페이 결제 준비 성공: tid=${body.result.tid}")
                        Log.d(TAG, "리디렉션 URL: ${body.result.nextRedirectMobileUrl}")
                        body
                    } else {
                        Log.e(TAG, "카카오페이 결제 준비 응답 본문이 null입니다")
                        KakaoPayReadyResponse(
                            isSuccess = false,
                            code = "NULL_RESPONSE",
                            message = "응답 데이터가 없습니다",
                            result = createEmptyReadyResult()
                        )
                    }
                } else {
                    Log.e(TAG, "카카오페이 결제 준비 실패: HTTP ${response.code()}")
                    val errorBody = response.errorBody()?.string() ?: ""
                    Log.e(TAG, "에러 응답: $errorBody")
                    
                    KakaoPayReadyResponse(
                        isSuccess = false,
                        code = "HTTP_${response.code()}",
                        message = "결제 준비 중 오류가 발생했습니다",
                        result = createEmptyReadyResult()
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "카카오페이 결제 준비 API 오류: ${e.message}", e)
                KakaoPayReadyResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "결제 준비 중 오류가 발생했습니다: ${e.message}",
                    result = createEmptyReadyResult()
                )
            }
        }
    }
    
    /**
     * 카카오페이 결제 성공 처리 API 호출
     * @param tid 결제 고유 번호
     * @param partnerOrderId 가맹점 주문번호
     * @param partnerUserId 가맹점 회원 ID
     * @param pgToken 결제승인 요청을 인증하는 토큰
     * @return KakaoPaySuccessResponse
     */
    suspend fun processKakaoPaymentSuccess(
        tid: String,
        partnerOrderId: String,
        partnerUserId: String,
        pgToken: String
    ): KakaoPaySuccessResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "카카오페이 결제 성공 처리 API 호출")
                Log.d(TAG, "tid=$tid, partnerOrderId=$partnerOrderId, partnerUserId=$partnerUserId")
                Log.d(TAG, "pgToken=${pgToken.take(10)}...")
                
                val request = KakaoPaySuccessRequest(
                    tid = tid,
                    partnerOrderId = partnerOrderId,
                    partnerUserId = partnerUserId,
                    pgToken = pgToken
                )
                
                val response = kakaoPayApi.processPaymentSuccess(request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d(TAG, "카카오페이 결제 성공 처리 완료: isSuccess=${body.isSuccess}")
                        body
                    } else {
                        Log.e(TAG, "카카오페이 결제 성공 처리 응답 본문이 null입니다")
                        KakaoPaySuccessResponse(
                            isSuccess = false,
                            code = "NULL_RESPONSE",
                            message = "응답 데이터가 없습니다",
                            result = null
                        )
                    }
                } else {
                    Log.e(TAG, "카카오페이 결제 성공 처리 실패: HTTP ${response.code()}")
                    val errorBody = response.errorBody()?.string() ?: ""
                    Log.e(TAG, "에러 응답: $errorBody")
                    
                    KakaoPaySuccessResponse(
                        isSuccess = false,
                        code = "HTTP_${response.code()}",
                        message = "결제 처리 중 오류가 발생했습니다",
                        result = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "카카오페이 결제 성공 처리 API 오류: ${e.message}", e)
                KakaoPaySuccessResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "결제 처리 중 오류가 발생했습니다: ${e.message}",
                    result = null
                )
            }
        }
    }
    
    /**
     * 사용자 정보 갱신 (구독 상태 확인)
     * @param token 인증 토큰
     * @return UserInfoResponse
     */
    suspend fun refreshUserInfo(token: String): UserInfoResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "사용자 정보 갱신 API 호출")
                authApi.getUserInfo("Bearer $token")
            } catch (e: Exception) {
                Log.e(TAG, "사용자 정보 갱신 실패: ${e.message}", e)
                UserInfoResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "사용자 정보 조회 실패: ${e.message}",
                    result = com.example.maite.UserInfoResult(
                        userId = 0L,
                        email = "",
                        name = ""
                    )
                )
            }
        }
    }
    
    /**
     * 빈 KakaoPayReadyResult 생성
     */
    private fun createEmptyReadyResult() = com.example.maite.model.KakaoPayReadyResult(
        tid = "",
        partnerOrderId = "",
        partnerUserId = "",
        nextRedirectMobileUrl = ""
    )
}
