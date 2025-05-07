package com.example.maite.data.model

import com.google.gson.annotations.SerializedName

data class ProposalResponse(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("type")
    val type: ProposalType?, // null 허용하도록 변경
    
    @SerializedName("title")
    val title: String?,     // null 허용
    
    @SerializedName("fromUser")
    val fromUser: String?, // null 허용
    
    @SerializedName("fromUserId")
    val fromUserId: Int,
    
    // 회의 관련 필드 (type이 MEETING일 때 사용)
    @SerializedName("date")
    val date: String?, // "2025-04-05" 형식
    
    @SerializedName("startTime")
    val startTime: String?, // "13:00" 형식
    
    @SerializedName("endTime")
    val endTime: String?, // "14:00" 형식
    
    @SerializedName("location")
    val location: String?,
    
    // 회의방 초대 관련 필드 (type이 ROOM_INVITE일 때 사용)
    @SerializedName("roomId")
    val roomId: Int?,
    
    @SerializedName("roomName")
    val roomName: String?,
    
    @SerializedName("createdAt")
    val createdAt: String?,  // null 허용
    
    @SerializedName("status")
    val status: String?     // null 허용, "PENDING", "ACCEPTED", "REJECTED"
)
