package com.example.maite.model

import android.content.Context
import android.util.Log
import com.example.maite.MaiteRetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MaiteListRepository(private val context: Context) {

    // 기존 getMaiteList 메서드 수정: roomId 필드 포함
    suspend fun getMaiteList(): List<MaiteListItem> {
        return withContext(Dispatchers.IO) {
            try {
                // Context를 전달하여 Retrofit 서비스 인스턴스 가져오기
                val service = MaiteRetrofitClient.getInstance(context)
                val response = service.getMyRooms() // API 호출

                if (response.isSuccessful) {
                    val roomList = response.body() ?: emptyList()
                    Log.d("MaiteListRepository", "API 응답 성공: ${roomList.size}개 항목 수신")
                    // RoomItem 리스트를 MaiteListItem 리스트로 변환
                    return@withContext roomList.map { room ->
                        MaiteListItem(
                            roomId = room.roomId,    // roomId 필드 추가
                            title = room.name,       // Room의 name을 title로 사용
                            name = room.hostEmail,   // Room의 hostEmail을 name으로 사용
                            intro = room.description // Room의 description을 intro로 사용
                        )
                    }
                } else {
                    // API 호출 실패 처리
                    Log.e("MaiteListRepository", "API 호출 실패: ${response.code()} ${response.message()}")
                    return@withContext emptyList() // 실패 시 빈 리스트 반환
                }
            } catch (e: Exception) {
                // 네트워크 오류 등 예외 처리
                Log.e("MaiteListRepository", "API 호출 중 오류 발생", e)
                return@withContext emptyList() // 오류 발생 시 빈 리스트 반환
            }
        }
    }

    // 방 상세 정보를 가져오는 메서드 추가
    suspend fun getRoomDetail(roomId: Long): RoomItem {
        return withContext(Dispatchers.IO) {
            try {
                val service = MaiteRetrofitClient.getInstance(context)
                val response = service.getRoomDetail(roomId)

                if (response.isSuccessful) {
                    val roomDetail = response.body()
                    if (roomDetail != null) {
                        Log.d("MaiteListRepository", "방 상세 정보 가져오기 성공: $roomDetail")
                        return@withContext roomDetail
                    } else {
                        Log.e("MaiteListRepository", "방 상세 정보가 null입니다")
                        throw Exception("방 상세 정보가 없습니다")
                    }
                } else {
                    Log.e("MaiteListRepository", "방 상세 정보 API 호출 실패: ${response.code()} ${response.message()}")
                    throw Exception("방 상세 정보를 가져오는데 실패했습니다: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MaiteListRepository", "방 상세 정보 가져오기 중 오류 발생", e)
                throw e  // 상위 레벨에서 오류 처리할 수 있도록 예외 전파
            }
        }
    }
}