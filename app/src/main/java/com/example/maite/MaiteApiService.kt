package com.example.maite

import com.example.maite.model.RoomItem
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
}