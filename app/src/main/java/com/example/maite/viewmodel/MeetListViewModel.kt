package com.example.maite.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.maite.model.MeetListItem // 데이터 모델 import

class MeetListViewModel : ViewModel() {

    // 임시 데이터 소스 (실제 앱에서는 Repository 등을 통해 가져옵니다)
    private val _meetList = MutableLiveData<List<MeetListItem>>()
    val meetList: LiveData<List<MeetListItem>> = _meetList

    init {
        loadMeetList() // ViewModel 생성 시 데이터 로드
    }

    private fun loadMeetList() {
        // 여기에 실제 데이터 로직 구현 (예: 네트워크 요청, 데이터베이스 조회)
        // 임시 데이터 생성 (id 제거됨)
        val sampleData = listOf(
            MeetListItem("산학 프로젝트 회의 1차", "2025.03.02", "08:00 ~ 10:00", "경기도 성남시 어쩌구 저쩌구"),
            MeetListItem("팀 스터디", "2025.03.05", "14:00 ~ 16:00", "온라인 (Zoom)"),
            MeetListItem("아이디어 구체화 회의", "2025.03.10", "10:00 ~ 11:30", "회사 회의실 A"),
            MeetListItem("중간 결과 발표 준비", "2025.03.15", "13:00 ~ 15:00", "스터디 카페")
        )
        _meetList.value = sampleData
    }

    // 필요한 경우 데이터 추가/삭제/수정 함수 구현
}