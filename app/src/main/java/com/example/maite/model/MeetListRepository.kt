package com.example.maite.model

class MeetListRepository {
    fun getMeetList(): List<MeetListItem> {
        return listOf(
            MeetListItem("산학 프로젝트 회의 1차", "2025.03.02", "08:00 ~ 10:00", "경기도 성남시 어쩌구 저쩌구"),
            MeetListItem("팀 스터디", "2025.03.05", "14:00 ~ 16:00", "온라인 (Zoom)"),
            MeetListItem("아이디어 구체화 회의", "2025.03.10", "10:00 ~ 11:30", "회사 회의실 A"),
            MeetListItem("중간 결과 발표 준비", "2025.04.15", "13:00 ~ 15:00", "스터디 카페"),
            MeetListItem("어버이날 선물", "2025.05.06", "13:00 ~ 14:00", "스타벅스")
        )
    }
}