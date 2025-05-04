package com.example.maite.model

class MaiteListRepository {

    fun getMaiteList(): List<MaiteListItem> {
        // 시연을 위한 더미 데이터
        return listOf(
            MaiteListItem("김정훈의 MAITE", "김정훈", "산학 프로젝트 파이팅!"),
            MaiteListItem("안성진의 MAITE", "안성진", "안드로이드 개발 중!"),
            MaiteListItem("이민우의 MAITE", "이민우", "UI 디자인 작업 중!"),
            MaiteListItem("박지원의 MAITE", "박지원", "백엔드 개발 진행 중!")
        )
    }
}