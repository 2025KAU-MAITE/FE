package com.example.maite.repository

import android.content.Context
import com.example.maite.ApiClient
import com.example.maite.model.AuthApi
import com.example.maite.model.UserInfo
import com.example.maite.PreferencesUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.maite.model.User  // User 클래스 import 필요

class UserRepository(private val context: Context) {

    private val TAG = "UserRepository"
    private val authApi = ApiClient.getClient(context).create(AuthApi::class.java)
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
    fun getMockUsers(query: String): List<User> {
        val mockUsers = listOf(
            User("1", "김정훈", "https://randomuser.me/api/portraits/men/1.jpg", false),
            User("2", "김정훈", "https://randomuser.me/api/portraits/men/2.jpg", false),
            User("3", "김정훈", "https://randomuser.me/api/portraits/men/3.jpg", false),
            User("4", "김정훈", "https://randomuser.me/api/portraits/men/4.jpg", false)
        )

        return if (query.isEmpty()) emptyList() else mockUsers.filter { it.name.contains(query) }
    }

}