package com.example.maite.api

import com.example.maite.model.User
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface UserApiService {
    
    @GET("users/search")
    suspend fun searchUsers(@Query("query") query: String): Response<List<User>>
    
    @POST("users/friend-requests")
    suspend fun sendFriendRequests(@Query("userIds") userIds: List<String>): Response<Boolean>
}