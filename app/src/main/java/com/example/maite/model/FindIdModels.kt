package com.example.maite.model

// 아이디 찾기 - 인증번호 발송 요청 모델
data class FindIdSendRequest(
    val name: String,
    val phonenumber: String
)

// 아이디 찾기 - 인증번호 확인 요청 모델
data class FindIdVerifyRequest(
    val name: String,
    val phonenumber: String,
    val verificationCode: String
)

// 아이디 찾기 - 응답 모델
data class FindIdResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: FindIdResult
)

// 아이디 찾기 - 결과 모델
data class FindIdResult(
    val status: Boolean = true,
    val email: String,
    val message: String
)