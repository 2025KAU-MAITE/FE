package com.example.maite.model

data class SmsAuthSendRequest(
    val phoneNumber: String
)

data class SmsAuthVerifyRequest(
    val phoneNumber: String,
    val verificationCode: String
)

data class SmsAuthResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: SmsAuthResult
)

data class SmsAuthResult(
    val message: String,
    val status: Boolean = false // 인증 성공 여부 (verify API에서 사용)
)