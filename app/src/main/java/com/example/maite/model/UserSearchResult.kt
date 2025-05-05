package com.example.maite.model

import com.google.gson.annotations.SerializedName

/**
 * 사용자 검색 결과 모델 클래스
 */
data class UserSearchResult(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("profile_image") val profileImage: String? = null,
    var isSelected: Boolean = false
)

/**
 * 사용자 검색 응답 모델 클래스
 */
data class UserSearchResponse(
    @SerializedName("users") val users: List<UserSearchResult>,
    @SerializedName("total_count") val totalCount: Int
)