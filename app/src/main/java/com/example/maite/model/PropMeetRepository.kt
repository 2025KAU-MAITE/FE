package com.example.maite.model

class PropMeetRepository {

    fun getProposedMeetings(): List<PropMeetItem> {
        return listOf(
            PropMeetItem("산학 프로젝트 회의", "2025.03.02", "08:00 ~ 10:00", "경기도 성남시 어쩌구 저쩌구"),
            PropMeetItem("팀 스터디", "2025.03.05", "14:00 ~ 15:00", "온라인 (Zoom)"),
            PropMeetItem("아이디어 구체화 회의", "2025.03.10", "10:00 ~ 11:30", "본관 3층 회의실")
        )
    }
}