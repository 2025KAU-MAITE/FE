package com.example.maite.model

// DTO for sending reset password code
data class ResetPasswordSendRequest(
    val name: String,
    val email: String,
    val phonenumber: String
)

// DTO for verifying the reset password code
data class ResetPasswordVerifyRequest(
    val phonenumber: String,
    val verificationCode: String
)

// DTO for updating the password
data class ResetPasswordUpdateRequest(
    val email: String,
    val password: String
)

// API response for reset password operations
data class ResetPasswordResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: ResetPasswordResult
)

data class ResetPasswordResult(
    val status: Boolean = false,
    val message: String = "",
    val code: String = "",
    val email: String = ""
)
