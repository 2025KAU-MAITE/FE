package com.example.maite.model

import android.content.Context // Context import 추가
import android.util.Log
import com.example.maite.MaiteRetrofitClient // Retrofit 클라이언트 import

// Context를 생성자에서 받도록 수정
class MaiteListRepository(private val context: Context) {

    // API 호출 함수로 변경 (suspend 함수로 변경)
    suspend fun getMaiteList(): List<MaiteListItem> {
        try {
            // Context를 전달하여 Retrofit 서비스 인스턴스 가져오기
            val service = MaiteRetrofitClient.getInstance(context)
            val response = service.getMyRooms() // API 호출

            if (response.isSuccessful) {
                val roomList = response.body() ?: emptyList()
                Log.d("MaiteListRepository", "API 응답 성공: ${roomList.size}개 항목 수신")
                // RoomItem 리스트를 MaiteListItem 리스트로 변환
                return roomList.map { room ->
                    MaiteListItem(
                        title = room.name,       // Room의 name을 title로 사용
                        name = room.hostEmail,   // Room의 hostEmail을 name으로 사용
                        intro = room.description // Room의 description을 intro로 사용
                        // 만약 RoomItem에 ID가 있다면 MaiteListItem에도 ID 필드 추가 고려
                    )
                }
            } else {
                // API 호출 실패 처리
                Log.e("MaiteListRepository", "API 호출 실패: ${response.code()} ${response.message()}")
                return emptyList() // 실패 시 빈 리스트 반환
            }
        } catch (e: Exception) {
            // 네트워크 오류 등 예외 처리
            Log.e("MaiteListRepository", "API 호출 중 오류 발생", e)
            return emptyList() // 오류 발생 시 빈 리스트 반환
        }
    }
}