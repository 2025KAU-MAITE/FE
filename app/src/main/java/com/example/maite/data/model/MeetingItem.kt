package com.example.maite.data.model

import com.google.gson.annotations.SerializedName

data class MeetingItem(
    @SerializedName("meetingId")
    val id: Int,
    
    val title: String,
    
    @SerializedName("meetingDate")
    val date: String,      // 예: "2025-05-30" (yyyy-MM-dd 형식)
    
    @SerializedName("meetingTime")
    val startTime: String, // 예: "08:00" (HH:mm 형식)
    
    @SerializedName("address")
    val location: String,  // 장소
    
    @SerializedName("proposerName")
    val proposerName: String? = null, // 제안자 이름 (선택사항)
    
    val acceptance: Boolean? = null // 수락 여부 (선택사항)
) {
    // endTime은 스웨거에 없으므로 계산된 프로퍼티로 처리 (1시간 후로 가정)
    val endTime: String
        get() {
            return try {
                val parts = startTime.split(":")
                val hour = parts[0].toInt() + 1 // 1시간 더하기
                val minute = parts[1]
                String.format("%02d:%s", hour, minute)
            } catch (e: Exception) {
                "${startTime}" // 파싱 실패시 시작시간 반환
            }
        }
}
