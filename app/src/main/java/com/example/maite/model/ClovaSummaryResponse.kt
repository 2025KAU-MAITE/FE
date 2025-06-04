package com.example.maite.model

import com.google.gson.annotations.SerializedName

data class ClovaSummaryResponse(
    @SerializedName("isSuccess") val isSuccess: Boolean,
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
    @SerializedName("result") val result: ClovaSummaryResult
)

data class ClovaSummaryResult(
    @SerializedName("transcript") val transcript: String?,
    @SerializedName("result") val result: String?
)