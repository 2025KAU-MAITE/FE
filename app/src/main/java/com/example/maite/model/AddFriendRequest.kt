package com.example.maite.model

import com.google.gson.annotations.SerializedName

/**
 * 친구 추가 요청 모델
 * POST /api/mates API 요청에 사용됨
 */
data class AddFriendRequest(
    @SerializedName("userId") val userId: Long
)