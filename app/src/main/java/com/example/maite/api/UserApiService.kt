package com.example.maite.api

import com.example.maite.model.AddFriendRequest
import com.example.maite.model.ApiResponse
import com.example.maite.model.UserSearchResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface UserApiService {
    
    @GET("api/mates/search")
    suspend fun searchUsers(@Query("query") query: String): Response<UserSearchResponse>
    
    @POST("api/mates")
    suspend fun addFriend(@Body request: AddFriendRequest): Response<UserSearchResponse>
    
    @POST("api/mates/requests")
    suspend fun sendFriendRequest(@Body request: AddFriendRequest): Response<ApiResponse<Any?>>
    
    // 친구 요청 수락 API
    @POST("api/mates/requests/{requestId}/accept")
    suspend fun acceptFriendRequest(@Path("requestId") requestId: Int): Response<ApiResponse<Any?>>
    
    // 친구 요청 거절 API
    @POST("api/mates/requests/{requestId}/reject")
    suspend fun rejectFriendRequest(@Path("requestId") requestId: Int): Response<ApiResponse<Any?>>
    
    // 프로필 이미지 업로드 API
    @Multipart
    @POST("api/profile/image")
    suspend fun uploadProfileImage(
        @Part file: MultipartBody.Part
    ): Response<ApiResponse<Any?>>
    
    // 프로필 이미지 초기화 API
    @POST("api/profile/image-to-basic")
    suspend fun resetProfileImage(): Response<ApiResponse<Any?>>
    
    // 친구 삭제 API
    @retrofit2.http.DELETE("api/mates/{userId}")
    suspend fun deleteMate(@Path("userId") userId: Long): Response<ApiResponse<Any?>>
}