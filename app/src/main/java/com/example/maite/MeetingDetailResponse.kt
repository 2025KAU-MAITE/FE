package com.example.maite

import com.google.gson.annotations.SerializedName

/**
 * 회의 상세 정보 응답 모델
 */
data class MeetingDetailResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: MeetingDetailResult?
)

data class MeetingDetailResult(
    @SerializedName("meeting_id")
    val meetingId: Long,
    @SerializedName("meeting_title")
    val meetingTitle: String,
    @SerializedName("meeting_date")
    val meetingDate: String,
    @SerializedName("meeting_time")
    val meetingTime: String,
    @SerializedName("meeting_duration")
    val meetingDuration: Int,
    @SerializedName("participant_count")
    val participantCount: Int,
    @SerializedName("meeting_summary")
    val meetingSummary: String?,
    @SerializedName("meeting_transcript")
    val meetingTranscript: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String
)