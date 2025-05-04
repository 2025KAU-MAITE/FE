package com.example.maite.model

data class EmailCheckRequest(
    val email: String
)

data class EmailCheckResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: EmailCheckResult
)

data class EmailCheckResult(
    val message: String,
    val duplicated: Boolean
)