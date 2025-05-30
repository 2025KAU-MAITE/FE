package com.example.maite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PropMeetItem(
    val meetingId: Long,
    val title: String,
    val date: String,
    val time: String,
    val place: String,
    val acceptance: String
) : Parcelable