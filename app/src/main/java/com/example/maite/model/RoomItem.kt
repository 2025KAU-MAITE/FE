package com.example.maite.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Parcelable 추가 (ListDetailFragment로 전달될 가능성 고려)
@Parcelize
data class RoomItem(
    val roomId: Long, // JSON 응답이 0이므로 Long 또는 Int 사용 가능, 보통 ID는 Long
    val name: String,
    val hostEmail: String,
    val description: String
) : Parcelable