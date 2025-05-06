package com.example.maite.model

import com.google.gson.annotations.SerializedName
import java.time.ZonedDateTime

// API 응답의 공통 래퍼 구조
data class ApiResponse<T>(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: T
)

// Mate API 응답 모델
data class MateItem(
    val id: Long,
    val mateId: Long,
    val name: String,
    val email: String,
    val profileImageUrl: String?,
    val createdAt: ZonedDateTime
)

// InviteListItem으로 변환하는 확장 함수
fun MateItem.toInviteListItem(): InviteListItem {
    return InviteListItem(
        id = this.mateId,
        name = this.name,
        email = this.email,
        profileImageUrl = this.profileImageUrl
    )
}