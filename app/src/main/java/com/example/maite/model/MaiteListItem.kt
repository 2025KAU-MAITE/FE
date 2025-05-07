package com.example.maite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MaiteListItem(
    val roomId: Long? = null,
    val title: String,
    val name: String,
    val intro: String,
    val participantEmails: List<String> = emptyList()
) : Parcelable