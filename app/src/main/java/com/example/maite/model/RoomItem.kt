package com.example.maite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RoomItem(
    val roomId: Long,
    val name: String,
    val hostEmail: String,
    val description: String,
    val participantEmails: List<String>? = null
) : Parcelable