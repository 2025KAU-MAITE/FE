package com.example.maite.api

import com.example.maite.model.UserSearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface UserApiService {
    
    @GET("api/mates/search")
    suspend fun searchUsers(@Query("query") query: String): Response<UserSearchResponse>
    
    @POST("users/friend-requests")
    suspend fun sendFriendRequests(@Query("userIds") userIds: List<String>): Response<Boolean>
}