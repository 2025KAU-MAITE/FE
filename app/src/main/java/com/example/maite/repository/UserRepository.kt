package com.example.maite.repository

import com.example.maite.api.UserApiService
import com.example.maite.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val userApiService: UserApiService) {
    
    suspend fun searchUsers(query: String): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val response = userApiService.searchUsers(query)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Search failed with code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendFriendRequests(userIds: List<String>): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = userApiService.sendFriendRequests(userIds)
            if (response.isSuccessful) {
                Result.success(response.body() ?: false)
            } else {
                Result.failure(Exception("Friend requests failed with code: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // For development testing - return mock data
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