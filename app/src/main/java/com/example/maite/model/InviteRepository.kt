package com.example.maite.model

import com.example.maite.MaiteApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class InviteRepository(private val apiService: MaiteApiService) {

    // 서버에서 mate 목록 가져오기
    suspend fun getInviteList(): Result<List<InviteListItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMates()

            if (response.isSuccessful) {
                val mateResponse = response.body()
                if (mateResponse != null && mateResponse.isSuccess) {
                    // API 응답 데이터를 InviteListItem으로 변환
                    val inviteList = mateResponse.result.map { mate ->
                        mate.toInviteListItem()
                    }
                    Result.success(inviteList)
                } else {
                    Result.failure(Exception("API 응답 실패: ${mateResponse?.message ?: "알 수 없는 오류"}"))
                }
            } else {
                Result.failure(HttpException(response))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}