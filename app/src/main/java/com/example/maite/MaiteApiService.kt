package com.example.maite

import com.example.maite.model.ApiResponse
import com.example.maite.model.ChatListApiResponse
import com.example.maite.model.ClovaSummaryResponse
import com.example.maite.model.CreateMeetingRequest
import com.example.maite.model.MateItem
import com.example.maite.model.RoomItem
import com.example.maite.model.CreateRoomRequest
import com.example.maite.model.CreatedMeetingResponse
import com.example.maite.model.InviteUserRequest
import com.example.maite.model.MeetingDetailResponse
import com.example.maite.model.MeetingResponse
import com.example.maite.model.MessageApiResponse
import com.example.maite.model.SelectPlaceRequest
import com.example.maite.model.ServerMateItem
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface MaiteApiService {

    @Multipart
    @POST("api/AI/summary")
    suspend fun uploadAudioSummary(
        @Query("topic") topic: String,
        @Query("meeting") meetingId: Long,
        @Part file: MultipartBody.Part
    ): Response<ResponseBody>

    @Multipart
    @POST("api/AI/reply")
    suspend fun getAiReply(
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

    @POST("meetings/{meetingId}/invites/accept")
    suspend fun acceptMeetingInvite(
        @Path("meetingId") meetingId: Long
    ): Response<Void>

    @POST("meetings/{meetingId}/invites/reject")
    suspend fun rejectMeetingInvite(
        @Path("meetingId") meetingId: Long
    ): Response<Void>

    @GET("meetings/{meetingId}")
    suspend fun getMeetingDetail(
        @Path("meetingId") meetingId: Long
    ): Response<MeetingDetailResponse>

    @GET("api/mates/search")
    suspend fun searchUsers(@Query("query") email: String): Response<UserResponse>

    @POST("meetings/rooms/{roomId}")
    suspend fun createMeetingInRoom(
        @Path("roomId") roomId: Long,
        @Body request: CreateMeetingRequest
    ): Response<CreatedMeetingResponse>

    @PATCH("meetings/{meetingId}/select-place")
    suspend fun selectMeetingPlace(
        @Path("meetingId") meetingId: Long,
        @Body request: SelectPlaceRequest
    ): Response<Void>

    @Multipart
    @POST("api/AI/summary-clova")
    suspend fun uploadAudioSummaryClova(
        @Query("topic") topic: String,
        @Query("meeting") meetingId: Long,
        @Part file: MultipartBody.Part
    ): Response<ClovaSummaryResponse>
}