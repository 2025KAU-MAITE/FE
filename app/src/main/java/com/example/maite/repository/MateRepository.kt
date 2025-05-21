package com.example.maite.repository

import android.content.Context
import android.util.Log
import com.example.maite.ApiClient
import com.example.maite.MaiteApiService
import com.example.maite.model.MateItem
import com.example.maite.model.ServerMateItem
import com.example.maite.model.toMateItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class MateRepository(private val context: Context) {

    private val TAG = "MateRepository"
    private val apiService = ApiClient.getClient(context).create(MaiteApiService::class.java)

    // 친구 목록 가져오기
    suspend fun getMates(): List<MateItem> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMates()
                
                if (response.isSuccessful && response.body() != null) {
                    val responseBody = response.body()!!
                    val serverMateList = responseBody.result ?: emptyList()
                    
                    // ServerMateItem을 MateItem으로 변환
                    val mateList = serverMateList.map { serverMate: ServerMateItem ->
                        serverMate.toMateItem()
                    }
                    
                    Log.d(TAG, "친구 목록 조회 성공: ${mateList.size}명")
                    mateList
                } else {
                    Log.e(TAG, "친구 목록 조회 실패: ${response.errorBody()?.string()}")
                    emptyList()
                }
            } catch (e: HttpException) {
                Log.e(TAG, "친구 목록 조회 API 오류: ${e.code()}", e)
                emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "친구 목록 조회 중 오류 발생", e)
                emptyList()
            }
        }
    }
    
    // 친구 수 가져오기
    suspend fun getMateCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                val mates = getMates()
                mates.size
            } catch (e: Exception) {
                Log.e(TAG, "친구 수 조회 중 오류 발생", e)
                0
            }
        }
    }
}