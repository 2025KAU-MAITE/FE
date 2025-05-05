package com.example.maite.data.model

// UI에서 표시할 제안 모델 (회의 제안과 회의방 초대 통합)
data class MeetingProposal(
    val id: Int,
    val type: ProposalType,
    val title: String,
    val fromUser: String,  // 제안자 이름
    
    // 회의 제안인 경우
    val date: String? = null,      // 예: "2025.04.06"
    val time: String? = null,      // 예: "10:00 ~ 11:00"
    val location: String? = null,
    
    // 회의방 초대인 경우
    val roomId: Int? = null,
    val roomName: String? = null
)
