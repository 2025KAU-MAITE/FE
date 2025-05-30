package com.example.maite

import com.example.maite.model.ApiResponse
import com.example.maite.model.ChatListApiResponse
import com.example.maite.model.MateItem
import com.example.maite.model.RoomItem
import com.example.maite.model.CreateRoomRequest
import com.example.maite.model.InviteUserRequest
import com.example.maite.model.MeetingResponse
import com.example.maite.model.MessageApiResponse
import com.example.maite.model.ServerMateItem
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface MaiteApiService {

    @Multipart
    @POST("api/summary")
    suspend fun uploadAudioSummary(
        @Query("topic") topic: String,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    @GET("rooms")
    suspend fun getMyRooms(): Response<List<RoomItem>>

    @GET("api/mates")
    suspend fun getMates(): Response<ApiResponse<List<ServerMateItem>>>

    @POST("rooms")
    suspend fun createRoom(
        @Body request: CreateRoomRequest
    ): Response<ResponseBody>

    @GET("rooms/{roomId}")
    suspend fun getRoomDetail(
        @Path("roomId") roomId: Long
    ): Response<RoomItem>

    @POST("rooms/{roomId}/invites/")
    suspend fun inviteUserToRoom(
        @Path("roomId") roomId: Long,
        @Body request: InviteUserRequest
    ): Response<ResponseBody>

    @GET("/api/timetables/users/{userEmail}")
    suspend fun getTimetableByEmail(@Path("userEmail") userEmail: String): Response<RoomTimetableResponse>

    @GET("api/chats/rooms")
    suspend fun getChatRooms(): Response<ChatListApiResponse>

    @GET("api/chats/{roomId}/messages")
    suspend fun getChatMessages(
        @Path("roomId") roomId: Long,
        @Query("lastMessageId") lastMessageId: Long? = null
    ): Response<MessageApiResponse>

    @GET("meetings/rooms/{roomId}")
    suspend fun getMeetingsByRoom(@Path("roomId") roomId: Long): Response<List<MeetingResponse>>
}