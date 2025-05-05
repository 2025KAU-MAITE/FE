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
