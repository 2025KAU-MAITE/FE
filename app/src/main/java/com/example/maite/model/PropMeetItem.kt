package com.example.maite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PropMeetItem(
    val title: String,
    val date: String,
    val time: String,
    val place: String
) : Parcelable