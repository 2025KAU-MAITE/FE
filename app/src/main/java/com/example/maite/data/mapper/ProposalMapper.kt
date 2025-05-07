package com.example.maite.data.mapper

import com.example.maite.data.model.MeetingProposal
import com.example.maite.data.model.ProposalResponse
import com.example.maite.data.model.ProposalType
import android.util.Log

object ProposalMapper {
    private const val TAG = "ProposalMapper"

    fun mapToUiModel(response: ProposalResponse): MeetingProposal {
        // response.type이 null인 경우 로그를 남기고, roomId가 존재하면 ROOM_INVITE로 처리
        if (response.type == null) {
            Log.d(TAG, "응답의 type이 null입니다. roomId ${response.roomId}를 기반으로 타입 추론")
            
            // roomId가 있으면 회의방 초대로 처리
            return if (response.roomId != null) {
                Log.d(TAG, "roomId가 존재하므로 ROOM_INVITE로 처리합니다")
                MeetingProposal(
                    id = response.id,
                    type = ProposalType.ROOM_INVITE, // 명시적으로 타입 지정
                    title = "${response.fromUser ?: "알 수 없는 사용자"}님의 초대",
                    fromUser = response.fromUser ?: "알 수 없는 사용자",
                    roomId = response.roomId,
                    roomName = response.roomName ?: "알 수 없는 회의방"
                )
            } else {
                // 타입도 없고 roomId도 없다면 기본값으로 MEETING 처리
                Log.d(TAG, "type이 null이고 roomId도 없어 기본값 MEETING으로 처리합니다")
                MeetingProposal(
                    id = response.id,
                    type = ProposalType.MEETING, // 기본값
                    title = response.title ?: "제목 없음",
                    fromUser = response.fromUser ?: "알 수 없는 사용자",
                    date = formatDate(response.date),
                    time = formatTime(response.startTime, response.endTime),
                    location = response.location
                )
            }
        }
        
        // 기존 로직: type이 null이 아닌 경우
        return when (response.type) {
            ProposalType.MEETING -> {
                MeetingProposal(
                    id = response.id,
                    type = response.type,
                    title = response.title ?: "제목 없음",
                    fromUser = response.fromUser ?: "알 수 없는 사용자",
                    date = formatDate(response.date),
                    time = formatTime(response.startTime, response.endTime),
                    location = response.location
                )
            }
            ProposalType.ROOM_INVITE -> {
                MeetingProposal(
                    id = response.id,
                    type = response.type,
                    title = "${response.fromUser ?: "알 수 없는 사용자"}님의 초대",
                    fromUser = response.fromUser ?: "알 수 없는 사용자",
                    roomId = response.roomId,
                    roomName = response.roomName ?: "알 수 없는 회의방"
                )
            }
            else -> {
                // 알 수 없는 타입은 MEETING으로 기본 처리
                Log.d(TAG, "알 수 없는 타입: ${response.type}, 기본값 MEETING으로 처리")
                MeetingProposal(
                    id = response.id,
                    type = ProposalType.MEETING,
                    title = response.title ?: "제목 없음",
                    fromUser = response.fromUser ?: "알 수 없는 사용자",
                    date = formatDate(response.date),
                    time = formatTime(response.startTime, response.endTime),
                    location = response.location
                )
            }
        }
    }
    
    private fun formatDate(date: String?): String? {
        // "2025-04-05" -> "2025.04.05"
        return date?.replace("-", ".")
    }
    
    private fun formatTime(startTime: String?, endTime: String?): String? {
        // "13:00", "14:00" -> "13:00 ~ 14:00"
        return if (startTime != null && endTime != null) {
            "$startTime ~ $endTime"
        } else {
            null
        }
    }
}
