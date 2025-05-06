package com.example.maite.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class RoomDetailResponse(
    @SerializedName("roomId") val roomId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("hostEmail") val hostEmail: String,
    @SerializedName("participantEmails") val participantEmails: List<String>,
    @SerializedName("createdAt") val createdAt: Date
)