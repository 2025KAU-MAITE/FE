package com.example.maite.model

import android.util.Log
import com.example.maite.MaiteRetrofitClient

class MaiteListRepository {

    // API를 호출하여 MaiteListItem 리스트를 반환하는 suspend 함수
    suspend fun getMaiteList(): List<MaiteListItem> {
        return try {
            // Retrofit을 통해 getMyRooms API 호출
            val response = MaiteRetrofitClient.instance.getMyRooms()

            if (response.isSuccessful) {
                // 응답 성공 시: 응답 본문(List<RoomItem>)을 가져와서
                // 각 RoomItem을 MaiteListItem으로 변환(map)
                // 응답 본문이 null일 경우 빈 리스트 반환 (?: emptyList())
                response.body()?.map { roomItem ->
                    MaiteListItem(
                        title = roomItem.name,       // RoomItem의 name을 title로 사용
                        name = roomItem.hostEmail,   // RoomItem의 hostEmail을 name으로 사용 (혹은 적절한 필드 선택)
                        intro = roomItem.description // RoomItem의 description을 intro로 사용
                    )
                } ?: emptyList()
            } else {
                // 응답 실패 시: 에러 로그 출력 후 빈 리스트 반환
                Log.e("MaiteListRepository", "방 목록 가져오기 오류: ${response.code()} ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            // 네트워크 오류 등 예외 발생 시: 에러 로그 출력 후 빈 리스트 반환
            Log.e("MaiteListRepository", "방 목록 가져오기 중 예외 발생", e)
            emptyList()
        }
    }
}