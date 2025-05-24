package com.example.maite.model

import com.google.gson.annotations.SerializedName

data class MessageApiResponse(
    @SerializedName("isSuccess") val isSuccess: Boolean,
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
    @SerializedName("result") val result: List<MessageDto>
)

data class MessageDto(
    @SerializedName("id") val id: Long,
    @SerializedName("roomId") val roomId: Long,
    @SerializedName("senderId") val senderId: Long,
    @SerializedName("senderName") val senderName: String,
    @SerializedName("senderProfileImageUrl") val senderProfileImageUrl: String?,
    @SerializedName("content") val content: String,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("sendAt") val sendAt: String,
    @SerializedName("readCount") val readCount: Int,
    @SerializedName("totalMemberCount") val totalMemberCount: Int,
    @SerializedName("readByUsers") val readByUsers: List<ReadByUserDto>,
    @SerializedName("read") val isRead: Boolean
)

data class ReadByUserDto(
    @SerializedName("userId") val userId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("profileImageUrl") val profileImageUrl: String?
)