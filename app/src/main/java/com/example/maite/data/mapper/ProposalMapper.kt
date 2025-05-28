package com.example.maite.data.mapper

import com.example.maite.data.model.MeetingProposal
import com.example.maite.data.model.ProposalResponse
import com.example.maite.data.model.ProposalType
import android.util.Log

object ProposalMapper {
    private const val TAG = "ProposalMapper"

    fun mapToUiModel(response: ProposalResponse): MeetingProposal {
        // 디버깅을 위한 전체 응답 로깅
        Log.d(TAG, "API 응답 데이터: $response")
        Log.d(TAG, "meetingId: ${response.meetingId}, id: ${response.id}")
        Log.d(TAG, "proposerName: ${response.proposerName}, fromUser: ${response.fromUser}")
        Log.d(TAG, "date: ${response.date}, meetingDate: ${response.meetingDate}")
        Log.d(TAG, "startTime: ${response.startTime}, endTime: ${response.endTime}, meetingTime: ${response.meetingTime}")
        Log.d(TAG, "location: ${response.location}, meetingPlace: ${response.meetingPlace}, address: ${response.address}")

        // response.type이 null인 경우 로그를 남기고, roomId가 존재하면 ROOM_INVITE로 처리
        if (response.type == null) {
            Log.d(TAG, "응답의 type이 null입니다. roomId ${response.roomId}를 기반으로 타입 추론")

            // roomId가 있으면 회의방 초대로 처리
            return if (response.roomId != null) {
                Log.d(TAG, "roomId가 존재하므로 ROOM_INVITE로 처리합니다")

                // 회의방 초대 API 응답에 맞게 hostEmail과 name 필드 사용
                val hostName = response.hostEmail ?: response.fromUser ?: "알 수 없는 사용자"
                val roomName = response.name ?: "알 수 없는 회의방"

                MeetingProposal(
                    id = response.id,
                    type = ProposalType.ROOM_INVITE,
                    title = "${hostName}님의 회의방 초대",
                    fromUser = hostName,
                    roomId = response.roomId,
                    roomName = roomName
                )
            } else {
                // 타입도 없고 roomId도 없다면 기본값으로 MEETING 처리
                Log.d(TAG, "type이 null이고 roomId도 없어 기본값 MEETING으로 처리합니다")
                val result = MeetingProposal(
                    id = response.meetingId.takeIf { it != 0 } ?: response.id,
                    type = ProposalType.MEETING, // 기본값
                    title = response.title ?: "제목 없음",
                    fromUser = response.proposerName ?: response.fromUser ?: "알 수 없는 사용자",
                    date = formatDate(response.meetingDate ?: response.date),
                    time = formatTime(response.meetingTime, response.startTime, response.endTime),
                    location = response.address ?: response.meetingPlace ?: response.location
                )

                // 변환 결과 로그 출력
                Log.d(TAG, "변환 결과: id=${result.id}, title=${result.title}, fromUser=${result.fromUser}")
                Log.d(TAG, "변환 결과: date=${result.date}, time=${result.time}, location=${result.location}")

                return result
            }
        }

        // 기존 로직: type이 null이 아닌 경우
        val result = when (response.type) {
            ProposalType.MEETING -> {
                MeetingProposal(
                    id = response.meetingId.takeIf { it != 0 } ?: response.id,
                    type = response.type,
                    title = response.title ?: "제목 없음",
                    fromUser = response.proposerName ?: response.fromUser ?: "알 수 없는 사용자",
                    date = formatDate(response.meetingDate ?: response.date),
                    time = formatTime(response.meetingTime, response.startTime, response.endTime),
                    location = response.address ?: response.meetingPlace ?: response.location
                )
            }
            ProposalType.ROOM_INVITE -> {
                // 회의방 초대 API 응답에 맞게 hostEmail과 name 필드 사용
                val hostName = response.hostEmail ?: response.fromUser ?: "알 수 없는 사용자"
                val roomName = response.name ?: "알 수 없는 회의방"

                MeetingProposal(
                    id = response.id,
                    type = response.type,
                    title = "${hostName}님의 회의방 초대",
                    fromUser = hostName,
                    roomId = response.roomId,
                    roomName = roomName
                )
            }
            else -> {
                // 알 수 없는 타입은 MEETING으로 기본 처리
                Log.d(TAG, "알 수 없는 타입: ${response.type}, 기본값 MEETING으로 처리")
                MeetingProposal(
                    id = response.meetingId.takeIf { it != 0 } ?: response.id,
                    type = ProposalType.MEETING,
                    title = response.title ?: "제목 없음",
                    fromUser = response.proposerName ?: response.fromUser ?: "알 수 없는 사용자",
                    date = formatDate(response.meetingDate ?: response.date),
                    time = formatTime(response.meetingTime, response.startTime, response.endTime),
                    location = response.address ?: response.meetingPlace ?: response.location
                )
            }
        }

        // 변환 결과 로그 출력
        Log.d(TAG, "변환 결과: id=${result.id}, title=${result.title}, fromUser=${result.fromUser}")
        Log.d(TAG, "변환 결과: date=${result.date}, time=${result.time}, location=${result.location}")

        return result
    }

    private fun formatDate(date: String?): String? {
        // "2025-05-28" -> "2025.05.28"
        return date?.replace("-", ".")
    }

    private fun formatTime(meetingTime: String?, startTime: String?, endTime: String?): String? {
        // meetingTime이 있으면 그것을 우선 사용
        return when {
            !meetingTime.isNullOrEmpty() -> meetingTime
            startTime != null && endTime != null -> "$startTime ~ $endTime"
            else -> null
        }
    }
}