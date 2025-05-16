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
    
    @POST("users/friend-requests")
    suspend fun sendFriendRequests(@Query("userIds") userIds: List<String>): Response<Boolean>
    
    // 프로필 이미지 업로드 API 추가
    @Multipart
    @POST("api/users/{userId}/profile-image")
    suspend fun uploadProfileImage(
        @Path("userId") userId: Long,
        @Part image: MultipartBody.Part
    ): Response<ApiResponse<ProfileImageResult>>
    
    // 프로필 이미지 초기화 API 추가
    @POST("api/users/{userId}/reset-profile-image")
    suspend fun resetProfileImage(
        @Path("userId") userId: Long
    ): Response<ApiResponse<ProfileImageResult>>
}

// 프로필 이미지 업로드 결과
data class ProfileImageResult(
    val profileImageUrl: String
)