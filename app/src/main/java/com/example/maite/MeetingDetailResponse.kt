package com.example.maite

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 회의 상세 정보 응답 모델
 * /meetings/{meetingId} API 응답을 위한 데이터 클래스
 */
@Parcelize
data class MeetingDetailResponse(
    val meetingId: Long,
    val title: String,
    val proposerName: String,
    val meetingDate: String,
    val meetingTime: String,
    val address: String,
    val participantEmails: List<String>,
    val record: String?,          // 녹음 파일 경로 또는 ID (null이면 녹음 없음)
    val recordText: String?,      // 회의록 전체 텍스트 (null이면 회의록 없음)
    val textSum: String?,         // 요약본 텍스트 (null이면 요약본 없음)
    val createdAt: String         // 회의 생성 시간
) : Parcelable
