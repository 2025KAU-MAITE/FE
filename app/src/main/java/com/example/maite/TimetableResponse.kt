package com.example.maite.model.network

data class TimetableResponse(
    val isSuccess: Boolean,
    val code: Int,
    val message: String,
    val result: TimetableResult
)

data class TimetableResult(
    val id: Long,
    val userId: Long
)
