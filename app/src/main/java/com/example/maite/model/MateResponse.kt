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

// 서버에서 오는 Mate API 응답 모델
data class ServerMateItem(
    val id: Long,
    val mateId: Long,
    val name: String,
    val email: String,
    val profileImageUrl: String?,
    val createdAt: ZonedDateTime
)

// ServerMateItem을 MateItem으로 변환하는 확장 함수
fun ServerMateItem.toMateItem(): MateItem {
    return MateItem(
        id = this.id,
        userId = this.mateId,
        name = this.name,
        email = this.email,
        profileImageUrl = this.profileImageUrl
    )
}

// ServerMateItem을 InviteListItem으로 변환하는 확장 함수
fun ServerMateItem.toInviteListItem(): InviteListItem {
    return InviteListItem(
        id = this.mateId,
        name = this.name,
        email = this.email,
        profileImageUrl = this.profileImageUrl
    )
}