package com.example.maite.repository

import android.content.Context
import android.util.Log
import com.example.maite.ApiClient
import com.example.maite.PreferencesUtil
import com.example.maite.api.UserApiService
import com.example.maite.model.AuthApi
import com.example.maite.model.User
import com.example.maite.model.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class UserRepository(private val context: Context) {

    private val TAG = "UserRepository"
    private val authApi = ApiClient.getClient(context).create(AuthApi::class.java)
    private val userApiService = ApiClient.getClient(context).create(UserApiService::class.java)
    private val preferencesUtil = PreferencesUtil(context)

    suspend fun getUserInfo(userId: Long): UserInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val token = preferencesUtil.getAccessToken()
                if (token == null) {
                    Log.e(TAG, "토큰이 없습니다")
                    return@withContext null
                }

                Log.d(TAG, "사용자 정보 조회 API 호출: userId=$userId")
                val response = authApi.getUserInfo("Bearer $token")

                if (response.isSuccess) {
                    Log.d(TAG, "사용자 정보 조회 성공")
                    UserInfo(
                        name = response.result.name,
                        mateCount = 100, // TODO: 실제 API에서 mate count 받아오기
                        profileImageUrl = null // TODO: 실제 API에서 프로필 이미지 URL 받아오기
                    )
                } else {
                    Log.e(TAG, "사용자 정보 조회 실패: ${response.message}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "사용자 정보 조회 중 오류 발생", e)
                null
            }
        }
    }

    // 실제 API를 통한 사용자 검색 메소드
    suspend fun searchUsers(query: String): List<User> {
        return withContext(Dispatchers.IO) {
            try {
                val token = preferencesUtil.getAccessToken()
                if (token == null) {
                    Log.e(TAG, "토큰이 없습니다")
                    return@withContext emptyList()
                }

                Log.d(TAG, "사용자 검색 API 호출: query=$query")
                val response = userApiService.searchUsers(query)
                
                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    
                    if (searchResponse?.isSuccess == true) {
                        Log.d(TAG, "사용자 검색 성공: ${searchResponse.result.size}명 찾음")
                        // API 응답 구조에 맞게 User 객체 생성
                        return@withContext searchResponse.result.map { result ->
                            User(
                                id = result.id,
                                name = result.name,
                                email = result.email,
                                profileImageUrl = result.profileImageUrl ?: "",
                                isSelected = false
                            )
                        }
                    } else {
                        Log.e(TAG, "사용자 검색 실패: ${searchResponse?.message}")
                        emptyList()
                    }
                } else {
                    Log.e(TAG, "사용자 검색 API 호출 실패: ${response.errorBody()?.string()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "사용자 검색 중 오류 발생", e)
                if (e is HttpException) {
                    Log.e(TAG, "HTTP 오류 코드: ${e.code()}")
                }
                emptyList()
            }
        }
    }

    // 테스트용 모의 데이터
    fun getMockUsers(query: String): List<User> {
        val mockUsers = listOf(
            User("1", "김정훈", "jhkim@example.com", "https://randomuser.me/api/portraits/men/1.jpg", false),
            User("2", "이민수", "mslee@example.com", "https://randomuser.me/api/portraits/men/2.jpg", false),
            User("3", "박영희", "yhpark@example.com", "https://randomuser.me/api/portraits/women/3.jpg", false),
            User("4", "최지우", "jwchoi@example.com", "https://randomuser.me/api/portraits/women/4.jpg", false)
        )

        return if (query.isEmpty()) 
            emptyList() 
        else 
            mockUsers.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.email.contains(query, ignoreCase = true) 
            }
    }

    // 친구 요청 전송
    suspend fun sendFriendRequests(userIds: List<String>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val token = preferencesUtil.getAccessToken()
                if (token == null) {
                    Log.e(TAG, "토큰이 없습니다")
                    return@withContext false
                }
                
                Log.d(TAG, "친구 요청 API 호출: userIds=$userIds")
                val response = userApiService.sendFriendRequests(userIds)
                
                if (response.isSuccessful && response.body() == true) {
                    Log.d(TAG, "친구 요청 전송 성공")
                    true
                } else {
                    Log.e(TAG, "친구 요청 전송 실패: ${response.errorBody()?.string()}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "친구 요청 전송 중 오류 발생", e)
                false
            }
        }
    }
}