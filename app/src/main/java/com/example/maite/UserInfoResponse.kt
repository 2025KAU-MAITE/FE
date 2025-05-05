package com.example.maite

data class UserInfoResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: UserInfoResult
)

data class UserInfoResult(
    val userId: Long,
    val email: String,
    val name: String
)