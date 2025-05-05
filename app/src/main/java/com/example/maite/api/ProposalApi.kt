package com.example.maite.api

import com.example.maite.data.model.MeetingItem
import com.example.maite.data.model.ProposalResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ProposalApi {
    // 읽지 않은 제안 목록 조회 (회의 제안, 회의방 초대 모두 포함)
    @GET("notifications/meetings")
    suspend fun getUnreadProposals(): Response<List<ProposalResponse>>
    
    // 내 회의 목록 조회
    @GET("meetings")
    suspend fun getMyMeetings(): Response<List<MeetingItem>>

    // 회의 제안 수락
    @POST("meetings/{proposalId}/invites/accept")
    suspend fun acceptMeetingProposal(
        @Path("proposalId") proposalId: Int
    ): Response<Unit>

    // 회의 제안 거절
    @POST("meetings/{proposalId}/invites/reject")
    suspend fun rejectProposal(
        @Path("proposalId") proposalId: Int
    ): Response<Unit>
    
    // 회의방 초대 수락
    @POST("rooms/{roomId}/invites/accept")
    suspend fun acceptRoomInvite(
        @Path("roomId") roomId: Int
    ): Response<Unit>
    
    // 회의방 초대 거절
    @POST("rooms/{roomId}/invites/reject")
    suspend fun rejectRoomInvite(
        @Path("roomId") roomId: Int
    ): Response<Unit>
}
