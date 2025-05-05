package com.example.maite.data.mapper

import com.example.maite.data.model.MeetingProposal
import com.example.maite.data.model.ProposalResponse
import com.example.maite.data.model.ProposalType

object ProposalMapper {
    fun mapToUiModel(response: ProposalResponse): MeetingProposal {
        return when (response.type) {
            ProposalType.MEETING -> {
                MeetingProposal(
                    id = response.id,
                    type = response.type,
                    title = response.title,
                    fromUser = response.fromUser,
                    date = formatDate(response.date),
                    time = formatTime(response.startTime, response.endTime),
                    location = response.location
                )
            }
            ProposalType.ROOM_INVITE -> {
                MeetingProposal(
                    id = response.id,
                    type = response.type,
                    title = "${response.fromUser}님의 초대",
                    fromUser = response.fromUser,
                    roomId = response.roomId,
                    roomName = response.roomName
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
