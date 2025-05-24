package com.example.maite.model

import com.google.gson.annotations.SerializedName

/**
 * 카카오페이 결제 준비 API 요청 모델
 */
data class KakaoPayReadyRequest(
    @SerializedName("total_amount")
    val totalAmount: Int,
    @SerializedName("item_name")
    val itemName: String,
    val quantity: Int
)

/**
 * 카카오페이 결제 준비 API 응답 모델
 */
data class KakaoPayReadyResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: KakaoPayReadyResult
)

data class KakaoPayReadyResult(
    val tid: String,
    @SerializedName("partner_order_id")
    val partnerOrderId: String,
    @SerializedName("partner_user_id")
    val partnerUserId: String,
    @SerializedName("next_redirect_mobile_url")
    val nextRedirectMobileUrl: String
)

/**
 * 카카오페이 결제 성공 API 요청 모델
 */
data class KakaoPaySuccessRequest(
    val tid: String,
    @SerializedName("partner_order_id")
    val partnerOrderId: String,
    @SerializedName("partner_user_id")
    val partnerUserId: String,
    @SerializedName("pg_token")
    val pgToken: String
)

/**
 * 카카오페이 결제 성공 API 응답 모델
 */
data class KakaoPaySuccessResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: KakaoPaySuccessResult?
)

data class KakaoPaySuccessResult(
    val status: String?,
    val message: String?,
    @SerializedName("payment_id")
    val paymentId: String?
)
