package com.example.maite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MeetListItem(
    val meetingId: Long,
    val title: String,
    val date: String,
    val time: String,
    val place: String
) : Parcelable