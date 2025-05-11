package com.example.maite.model

class ChatListRepository {

    // 개인 채팅 목록 가져오기
    fun getPersonalChats(): List<ChatListItem> {
        // 실제 구현에서는 API 호출이나 DB 조회를 수행
        return listOf(
            ChatListItem(id = "1", name = "김정훈"),
            ChatListItem(id = "2", name = "박정훈"),
            ChatListItem(id = "3", name = "이정훈")
        )
    }

    // 단체 채팅 목록 가져오기
    fun getGroupChats(): List<ChatListItem> {
        // 실제 구현에서는 API 호출이나 DB 조회를 수행
        return listOf(
            ChatListItem(
                id = "g1",
                name = "프로젝트 회의",
                intro = "산학 프로젝트 파이팅!",
                isGroup = true
            ),
            ChatListItem(
                id = "g2",
                name = "스터디 모임",
                intro = "알고리즘 공부 매주 화요일",
                isGroup = true
            ),
            ChatListItem(
                id = "g3",
                name = "동아리 채팅방",
                intro = "다음 모임은 금요일 6시입니다",
                isGroup = true
            )
        )
    }

    // 검색어로 채팅방 찾기
    fun searchChats(query: String, isPersonal: Boolean): List<ChatListItem> {
        val chatList = if (isPersonal) getPersonalChats() else getGroupChats()
        return chatList.filter { it.name.contains(query, ignoreCase = true) }
    }
}