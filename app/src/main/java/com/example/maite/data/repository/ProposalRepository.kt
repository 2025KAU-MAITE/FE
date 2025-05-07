package com.example.maite.data.repository

import com.example.maite.api.ProposalApi
import com.example.maite.data.mapper.ProposalMapper
import com.example.maite.data.model.MeetingItem
import com.example.maite.data.model.MeetingProposal
import com.example.maite.data.model.ProposalType
import java.lang.Exception

class ProposalRepository(
    private val proposalApi: ProposalApi
) {
    private val TAG = "ProposalRepository"
    
    /**
     * 읽지 않은 제안 목록을 가져오는 함수
     */
    suspend fun getUnreadProposals(): Result<List<MeetingProposal>> {
        return try {
            val response = proposalApi.getUnreadProposals()
            android.util.Log.d(TAG, "API response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val body = response.body()
                android.util.Log.d(TAG, "API response body: $body")
                
                val proposals = body?.map { 
                    ProposalMapper.mapToUiModel(it)
                } ?: emptyList()
                Result.success(proposals)
            } else {
                android.util.Log.e(TAG, "API error: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Failed to load proposals: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Exception in getUnreadProposals", e)
            Result.failure(e)
        }
    }
    
    /**
     * 읽지 않은 회의방 초대 알림을 가져오는 함수
     */
    suspend fun getRoomInvites(): Result<List<MeetingProposal>> {
        return try {
            android.util.Log.d(TAG, "회의방 초대 가져오기 API 호출 시작")
            val response = proposalApi.getRoomInvites()
            android.util.Log.d(TAG, "회의방 초대 API 응답 코드: ${response.code()}, 성공여부: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val body = response.body()
                android.util.Log.d(TAG, "회의방 초대 API 응답 본문: $body")
                android.util.Log.d(TAG, "회의방 초대 개수: ${body?.size ?: 0}개")
                
                // 각 초대에 대한 상세 정보 로깅
                body?.forEachIndexed { index, invite ->
                    android.util.Log.d(TAG, "회의방 초대[$index] - ID: ${invite.id}, 유형: ${invite.type}, " +
                            "제목: ${invite.title}, 보낸사람: ${invite.fromUser}, 방ID: ${invite.roomId}, 방이름: ${invite.roomName}")
                }
                
                val proposals = body?.map { 
                    ProposalMapper.mapToUiModel(it)
                } ?: emptyList()
                
                android.util.Log.d(TAG, "회의방 초대 변환 완료: ${proposals.size}개")
                Result.success(proposals)
            } else {
                android.util.Log.e(TAG, "회의방 초대 API 오류: ${response.code()} - ${response.message()}")
                android.util.Log.e(TAG, "회의방 초대 API 오류 응답 본문: ${response.errorBody()?.string()}")
                Result.failure(Exception("회의방 초대 로드 실패: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "회의방 초대 API 호출 중 예외 발생", e)
            Result.failure(e)
        }
    }
    
    /**
     * 모든 읽지 않은 제안(회의 제안 + 회의방 초대)을 가져오는 함수
     */
    suspend fun getAllProposals(): Result<List<MeetingProposal>> {
        val meetingProposals = getUnreadProposals()
        val roomInvites = getRoomInvites()
        
        val allProposals = mutableListOf<MeetingProposal>()
        
        meetingProposals.onSuccess { proposals ->
            allProposals.addAll(proposals)
        }
        
        roomInvites.onSuccess { invites ->
            allProposals.addAll(invites)
        }
        
        return if (meetingProposals.isFailure && roomInvites.isFailure) {
            // 둘 다 실패한 경우
            android.util.Log.e(TAG, "Failed to load both meeting proposals and room invites")
            Result.failure(Exception("Failed to load proposals and invites"))
        } else {
            // 하나라도 성공한 경우 (일부라도 데이터를 보여주기 위함)
            android.util.Log.d(TAG, "Loaded ${allProposals.size} total proposals")
            Result.success(allProposals)
        }
    }
    
    /**
     * 내 회의 목록을 가져오는 함수
     */
    suspend fun getMyMeetings(): Result<List<MeetingItem>> {
        return try {
            val response = proposalApi.getMyMeetings()
            android.util.Log.d(TAG, "API response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val body = response.body()
                android.util.Log.d(TAG, "API response body: $body")
                
                Result.success(body ?: emptyList())
            } else {
                android.util.Log.e(TAG, "API error: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Failed to load meetings: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Exception in getMyMeetings", e)
            Result.failure(e)
        }
    }
    
    /**
     * 제안을 수락하는 함수
     */
    suspend fun acceptProposal(proposal: MeetingProposal): Result<Unit> {
        return try {
            android.util.Log.d(TAG, "Accepting proposal: $proposal")
            
            val response = when (proposal.type) {
                ProposalType.MEETING -> {
                    android.util.Log.d(TAG, "Accepting meeting proposal with ID: ${proposal.id}")
                    proposalApi.acceptMeetingProposal(proposal.id)
                }
                ProposalType.ROOM_INVITE -> {
                    if (proposal.roomId != null) {
                        android.util.Log.d(TAG, "Accepting room invite with ID: ${proposal.roomId}")
                        proposalApi.acceptRoomInvite(proposal.roomId)
                    } else {
                        android.util.Log.e(TAG, "Room ID is null")
                        return Result.failure(Exception("Failed to accept room invite: Room ID is null"))
                    }
                }
            }
            
            android.util.Log.d(TAG, "API response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                android.util.Log.e(TAG, "API error: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Failed to accept proposal: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Exception in acceptProposal", e)
            Result.failure(e)
        }
    }
    
    /**
     * 제안을 거절하는 함수
     */
    suspend fun rejectProposal(proposal: MeetingProposal): Result<Unit> {
        return try {
            android.util.Log.d(TAG, "Rejecting proposal: $proposal")
            
            val response = when (proposal.type) {
                ProposalType.MEETING -> {
                    android.util.Log.d(TAG, "Rejecting meeting proposal with ID: ${proposal.id}")
                    proposalApi.rejectProposal(proposal.id)
                }
                ProposalType.ROOM_INVITE -> {
                    if (proposal.roomId != null) {
                        android.util.Log.d(TAG, "Rejecting room invite with ID: ${proposal.roomId}")
                        proposalApi.rejectRoomInvite(proposal.roomId)
                    } else {
                        android.util.Log.e(TAG, "Room ID is null")
                        return Result.failure(Exception("Failed to reject room invite: Room ID is null"))
                    }
                }
            }
            
            android.util.Log.d(TAG, "API response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                android.util.Log.e(TAG, "API error: ${response.code()} - ${response.message()}")
                Result.failure(Exception("Failed to reject proposal: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Exception in rejectProposal", e)
            Result.failure(e)
        }
    }
}
