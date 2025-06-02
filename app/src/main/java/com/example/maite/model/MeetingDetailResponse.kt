package com.example.maite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MeetingDetailResponse(
    val meetingId: Long,
    val title: String,
    val proposerName: String,
    val meetingDate: String,
    val meetingTime: String,
    val address: String,
    val participantEmails: List<String>,
    val record: String?,
    val recordText: String?,
    val textSum: String?,
    val createdAt: String
) : Parcelable