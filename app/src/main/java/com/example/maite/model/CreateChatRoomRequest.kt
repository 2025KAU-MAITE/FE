package com.example.maite.model

import com.google.gson.annotations.SerializedName

data class CreateChatRoomRequest(
    @SerializedName("receiverId") val receiverId: Long
)