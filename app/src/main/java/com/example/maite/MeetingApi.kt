package com.example.maite

import com.example.maite.data.model.MeetingItem
import com.example.maite.data.model.MeetingProposal
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MeetingApi {

    // ✅ 내 회의 목록 가져오기 (GET /meetings)
    @GET("meetings/my")
    suspend fun getMyMeetings(): Response<List<MeetingItem>>
    
    // ✅ 받은 제안 목록 가져오기 (GET /proposals)
    @GET("proposals/pending")
    suspend fun getPendingProposals(): Response<List<MeetingProposal>>
    
    // ✅ 회의 제안 수락하기 (POST /proposals/{id}/accept)
    @POST("proposals/{id}/accept")
    suspend fun acceptMeetingProposal(@Path("id") proposalId: Int): Response<Void>
    
    // ✅ 회의 제안 거절하기 (POST /proposals/{id}/decline)
    @POST("proposals/{id}/decline")
    suspend fun declineMeetingProposal(@Path("id") proposalId: Int): Response<Void>
    
    // ✅ 회의방 초대 수락하기 (POST /rooms/{roomId}/join)
    @POST("rooms/{roomId}/join")
    suspend fun joinRoom(@Path("roomId") roomId: Int): Response<Void>
    
    // 🆕 회의 제안 보내기 (POST /meetings/rooms/{roomId})
    @POST("meetings/rooms/{roomId}")
    suspend fun sendMeetingProposal(
        @Path("roomId") roomId: Int,
        @Body proposal: MeetingProposalRequest
    ): Response<Void>
}

// 🆕 회의 제안 요청 데이터 모델
data class MeetingProposalRequest(
    val title: String,
    val meetingDate: String,  // "2025-05-29" 형식
    val meetingTime: String,  // "14:00" 형식
    val inviteEmails: List<String>,
    val address: String  // 🆕 장소 필드 추가
)
