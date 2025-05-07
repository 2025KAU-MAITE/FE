package com.example.maite.model

import com.google.gson.annotations.SerializedName

/**
 * 사용자 검색 결과 모델 클래스
 */
data class UserSearchResult(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("profileImageUrl") val profileImageUrl: String? = null,
    var isSelected: Boolean = false
)

/**
 * 사용자 검색 응답 모델 클래스
 */
data class UserSearchResponse(
    @SerializedName("isSuccess") val isSuccess: Boolean,
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
    @SerializedName("result") val result: List<UserSearchResult>
)