package com.example.maite.repository

import android.util.Log
import com.example.maite.api.UserApiService
import com.example.maite.model.UserSearchResponse
import com.example.maite.model.UserSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 사용자 검색 관련 리포지토리
 */
class SearchUserRepository(private val userApiService: UserApiService) {

    private val TAG = "SearchUserRepository"

    // 사용자 검색
    suspend fun searchUsers(query: String): List<UserSearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val response = userApiService.searchUsers(query)
                if (response.isSuccessful && response.body() != null) {
                    val userResponse = response.body() as UserSearchResponse
                    Log.d(TAG, "사용자 ${userResponse.users.size}명 검색됨")
                    userResponse.users
                } else {
                    Log.e(TAG, "사용자 검색 실패: ${response.code()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "사용자 검색 중 오류", e)
                emptyList()
            }
        }
    }

    // 친구 요청 보내기
    suspend fun sendFriendRequests(userIds: List<String>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = userApiService.sendFriendRequests(userIds)
                val success = response.isSuccessful
                
                if (success) {
                    Log.d(TAG, "${userIds.size}명에게 친구 요청 전송 성공")
                } else {
                    Log.e(TAG, "친구 요청 전송 실패: ${response.code()}")
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "친구 요청 전송 중 오류", e)
                false
            }
        }
    }
}