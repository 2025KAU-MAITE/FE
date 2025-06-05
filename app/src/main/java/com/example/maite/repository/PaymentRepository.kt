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
            // 고유한 주문 ID와 사용자 ID 생성
            val partnerOrderId = "MAITE_${System.currentTimeMillis()}"
            val partnerUserId = "USER_${System.currentTimeMillis()}"
            
            try {
                Log.d(TAG, "카카오페이 결제 준비 API 호출: totalAmount=$totalAmount, itemName=$itemName, quantity=$quantity")
                Log.d(TAG, "생성된 partnerOrderId: $partnerOrderId")
                Log.d(TAG, "생성된 partnerUserId: $partnerUserId")
                
                val request = KakaoPayReadyRequest(
                    totalAmount = totalAmount,
                    itemName = itemName,
                    quantity = quantity,
                    partnerOrderId = partnerOrderId,
                    partnerUserId = partnerUserId
                )
                
                val response = kakaoPayApi.readyPayment(request)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        // Raw 응답을 JSON으로 출력하여 실제 필드명 확인
                        Log.d(TAG, "Raw 응답: ${response.raw()}")
                        try {
                            val gson = com.google.gson.Gson()
                            val jsonString = gson.toJson(body)
                            Log.d(TAG, "응답 JSON: $jsonString")
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON 변환 실패", e)
                        }
                        
                        Log.d(TAG, "카카오페이 결제 준비 성공: tid=${body.result.tid}")
                        Log.d(TAG, "응답에서 받은 partnerOrderId: ${body.result.partnerOrderId}")
                        Log.d(TAG, "응답에서 받은 partnerUserId: ${body.result.partnerUserId}")
                        Log.d(TAG, "리디렉션 URL: ${body.result.nextRedirectMobileUrl}")
                        
                        // 서버에서 partnerOrderId, partnerUserId를 반환하지 않는 경우를 대비해 
                        // 요청에서 사용한 값으로 덮어쓰기
                        val fixedResult = body.result.copy(
                            partnerOrderId = if (body.result.partnerOrderId.isNullOrEmpty()) partnerOrderId else body.result.partnerOrderId,
                            partnerUserId = if (body.result.partnerUserId.isNullOrEmpty()) partnerUserId else body.result.partnerUserId
                        )
                        
                        Log.d(TAG, "수정된 partnerOrderId: ${fixedResult.partnerOrderId}")
                        Log.d(TAG, "수정된 partnerUserId: ${fixedResult.partnerUserId}")
                        
                        body.copy(result = fixedResult)
                    } else {
                        Log.e(TAG, "카카오페이 결제 준비 응답 본문이 null입니다")
                        KakaoPayReadyResponse(
                            isSuccess = false,
                            code = "NULL_RESPONSE",
                            message = "응답 데이터가 없습니다",
                            result = createEmptyReadyResult(partnerOrderId, partnerUserId)
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
                        result = createEmptyReadyResult(partnerOrderId, partnerUserId)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "카카오페이 결제 준비 API 오류: ${e.message}", e)
                KakaoPayReadyResponse(
                    isSuccess = false,
                    code = "ERROR",
                    message = "결제 준비 중 오류가 발생했습니다: ${e.message}",
                    result = createEmptyReadyResult(partnerOrderId, partnerUserId)
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
                
                Log.d(TAG, "API 요청 데이터: $request")
                
                val response = kakaoPayApi.processPaymentSuccess(request)
                
                Log.d(TAG, "HTTP 응답 코드: ${response.code()}")
                Log.d(TAG, "HTTP 응답 메시지: ${response.message()}")
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d(TAG, "카카오페이 결제 성공 처리 완료: isSuccess=${body.isSuccess}")
                        Log.d(TAG, "응답 본문: $body")
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
                    Log.e(TAG, "에러 응답 본문: $errorBody")
                    Log.e(TAG, "요청 URL: ${response.raw().request.url}")
                    Log.e(TAG, "요청 헤더: ${response.raw().request.headers}")
                    
                    KakaoPaySuccessResponse(
                        isSuccess = false,
                        code = "HTTP_${response.code()}",
                        message = "결제 처리 중 오류가 발생했습니다: $errorBody",
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
    private fun createEmptyReadyResult(partnerOrderId: String = "", partnerUserId: String = "") = com.example.maite.model.KakaoPayReadyResult(
        tid = "",
        partnerOrderId = partnerOrderId,
        partnerUserId = partnerUserId,
        nextRedirectMobileUrl = ""
    )
}
