package com.example.maite.repository

import com.example.maite.MeetingApi
import com.example.maite.data.model.MeetingItem
import com.example.maite.data.model.MeetingProposal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 회의 및 제안 관련 API 처리를 담당하는 Repository
 */
class MeetingRepository(private val meetingApi: MeetingApi) {
    
    /**
     * 사용자의 회의 목록을 가져옵니다
     */
    suspend fun getMyMeetings(): Result<List<MeetingItem>> = withContext(Dispatchers.IO) {
        try {
            val response = meetingApi.getMyMeetings()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("회의 목록을 가져오는데 실패했습니다: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 대기 중인 제안 목록(회의 제안, 회의방 초대)를 가져옵니다
     */
    suspend fun getPendingProposals(): Result<List<MeetingProposal>> = withContext(Dispatchers.IO) {
        try {
            val response = meetingApi.getPendingProposals()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("제안 목록을 가져오는데 실패했습니다: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 회의 제안을 수락합니다
     */
    suspend fun acceptMeetingProposal(proposalId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = meetingApi.acceptMeetingProposal(proposalId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("회의 제안 수락에 실패했습니다: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 회의 제안을 거절합니다
     */
    suspend fun declineMeetingProposal(proposalId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = meetingApi.declineMeetingProposal(proposalId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("회의 제안 거절에 실패했습니다: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 회의방에 참가합니다 (초대 수락)
     */
    suspend fun joinRoom(roomId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = meetingApi.joinRoom(roomId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("회의방 참가에 실패했습니다: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
