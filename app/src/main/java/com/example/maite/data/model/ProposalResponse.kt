package com.example.maite.data.model

import com.google.gson.annotations.SerializedName

data class ProposalResponse(
    @SerializedName("id")
    val id: Int = 0,
    
    @SerializedName("type")
    val type: ProposalType? = null,
    
    @SerializedName("title")
    val title: String? = null,
    
    @SerializedName("fromUser")
    val fromUser: String? = null,
    
    @SerializedName("fromUserId")
    val fromUserId: Int = 0,
    
    // 회의 관련 필드 (type이 MEETING일 때 사용)
    @SerializedName("date")
    val date: String? = null, // "2025-04-05" 형식
    
    @SerializedName("startTime")
    val startTime: String? = null, // "13:00" 형식
    
    @SerializedName("endTime")
    val endTime: String? = null, // "14:00" 형식
    
    @SerializedName("location")
    val location: String? = null,
    
    // 회의방 초대 관련 필드
    @SerializedName("roomId")
    val roomId: Int? = null,
    
    // 회의방 초대(/notifications/rooms) API용 필드
    @SerializedName("name")  // API 응답의 실제 필드명
    val name: String? = null,
    
    @SerializedName("hostEmail") // API 응답의 실제 필드명
    val hostEmail: String? = null,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("status")
    val status: String? = null
)
