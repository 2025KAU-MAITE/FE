package com.example.maite.model

import com.google.gson.annotations.SerializedName

data class ChatListApiResponse(
    @SerializedName("isSuccess") val isSuccess: Boolean,
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
    @SerializedName("result") val result: List<ChatRoomDto>
)

data class ChatRoomDto(
    @SerializedName("id") val id: Long,
    @SerializedName("userId") val userId: Long,
    @SerializedName("receiverIds") val receiverIds: List<Long>,
    @SerializedName("roomName") val roomName: String,
    @SerializedName("lastMessageContent") val lastMessageContent: String?,
    @SerializedName("lastMessageTime") val lastMessageTime: String?,
    @SerializedName("profileImageUrl") val profileImageUrl: String?,
    @SerializedName("participantCount") val participantCount: Int,
    @SerializedName("groupChat") val isGroupChat: Boolean
)