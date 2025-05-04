package com.example.maite.model

import com.google.gson.annotations.SerializedName

/**
 * 회원가입 API 요청 데이터 모델
 */
data class SignupRequest(
    val email: String,
    val password: String,
    val name: String,
    @SerializedName("phonenumber")  // API 명세에 맞게 phonenumber로 변경
    val phoneNumber: String,
    val address: String
)