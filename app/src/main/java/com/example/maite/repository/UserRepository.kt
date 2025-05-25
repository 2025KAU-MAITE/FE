package com.example.maite.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.maite.ApiClient
import com.example.maite.PreferencesUtil
import com.example.maite.api.UserApiService
import com.example.maite.model.AddFriendRequest
import com.example.maite.model.AuthApi
import com.example.maite.model.User
import com.example.maite.model.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream

class UserRepository(private val context: Context) {

    private val TAG = "UserRepository"
    private val authApi = ApiClient.getClient(context).create(AuthApi::class.java)
    private val userApiService = ApiClient.getClient(context).create(UserApiService::class.java)
    private val preferencesUtil = PreferencesUtil(context)

    suspend fun getUserInfo(userId: Long): UserInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val token = preferencesUtil.getAccessToken()
                if (token == null) {
                    Log.e(TAG, "토큰이 없습니다")
                    return@withContext null
                }

                Log.d(TAG, "사용자 정보 조회 API 호출: userId=$userId")
                val response = authApi.getUserInfo("Bearer $token")

                Log.d(TAG, "getUserInfo API 구체적 응답: ${response}")
                Log.d(TAG, "getUserInfo result 내용: ${response.result}")
                if (response.isSuccess) {
                    Log.d(TAG, "사용자 정보 조회 성공")
                    
                    // 서버 응답에서 profileImageUrl 필드가 있는지 확인
                    var profileImageUrl: String? = null
                    try {
                        // 응답에 profileImageUrl 필드가 있는지 시도
                        profileImageUrl = response.result.profileImageUrl
                        Log.d(TAG, "프로필 이미지 URL 찾음: $profileImageUrl")
                        
                        // null이 아닌 유효한 URL이 있으면 저장
                        if (!profileImageUrl.isNullOrEmpty()) {
                            preferencesUtil.setString("user_profile_image_url", profileImageUrl)
                            Log.d(TAG, "서버에서 받은 프로필 이미지 URL 저장: $profileImageUrl")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "응답에 profileImageUrl 필드가 없음, 캐시된 URL 사용", e)
                        // profileImageUrl 필드가 없으면 저장된 값 사용
                        profileImageUrl = preferencesUtil.getString("user_profile_image_url")
                        Log.d(TAG, "캐시된 프로필 이미지 URL: $profileImageUrl")
                    }
                    
                    // 요금제 정보 처리
                    val subscribed = response.result.subscribed
                    Log.d(TAG, "요금제 정보: subscribed=$subscribed")
                    
                    // 요금제 정보를 로컬에 저장
                    preferencesUtil.setUserPremiumStatus(subscribed)
                    
                    UserInfo(
                        name = response.result.name,
                        mateCount = 100, // TODO: 실제 API에서 mate count 받아오기
                        profileImageUrl = profileImageUrl,
                        subscribed = subscribed
                    )
                } else {
                    Log.e(TAG, "사용자 정보 조회 실패: ${response.message}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "사용자 정보 조회 중 오류 발생", e)
                null
            }
        }
    }

    // 실제 API를 통한 사용자 검색 메소드
    suspend fun searchUsers(query: String): List<User> {
        return withContext(Dispatchers.IO) {
            try {
                val token = preferencesUtil.getAccessToken()
                if (token == null) {
                    Log.e(TAG, "토큰이 없습니다")
                    return@withContext emptyList()
                }

                Log.d(TAG, "사용자 검색 API 호출: query=$query")
                val response = userApiService.searchUsers(query)
                
                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    
                    if (searchResponse?.isSuccess == true) {
                        Log.d(TAG, "사용자 검색 성공: ${searchResponse.result.size}명 찾음")
                        // API 응답 구조에 맞게 User 객체 생성
                        return@withContext searchResponse.result.map { result ->
                            User(
                                id = result.id,
                                name = result.name,
                                email = result.email,
                                profileImageUrl = result.profileImageUrl ?: "",
                                isSelected = false
                            )
                        }
                    } else {
                        Log.e(TAG, "사용자 검색 실패: ${searchResponse?.message}")
                        emptyList()
                    }
                } else {
                    Log.e(TAG, "사용자 검색 API 호출 실패: ${response.errorBody()?.string()}")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "사용자 검색 중 오류 발생", e)
                if (e is HttpException) {
                    Log.e(TAG, "HTTP 오류 코드: ${e.code()}")
                }
                emptyList()
            }
        }
    }

    // 테스트용 모의 데이터
    fun getMockUsers(query: String): List<User> {
        val mockUsers = listOf(
            User("1", "김정훈", "jhkim@example.com", "https://randomuser.me/api/portraits/men/1.jpg", false),
            User("2", "이민수", "mslee@example.com", "https://randomuser.me/api/portraits/men/2.jpg", false),
            User("3", "박영희", "yhpark@example.com", "https://randomuser.me/api/portraits/women/3.jpg", false),
            User("4", "최지우", "jwchoi@example.com", "https://randomuser.me/api/portraits/women/4.jpg", false)
        )

        return if (query.isEmpty()) 
            emptyList() 
        else 
            mockUsers.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.email.contains(query, ignoreCase = true) 
            }
    }

    // 친구 요청 전송
    suspend fun sendFriendRequest(userId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val token = preferencesUtil.getAccessToken()
                if (token == null) {
                    Log.e(TAG, "토큰이 없습니다")
                    return@withContext false
                }
                
                Log.d(TAG, "친구 요청 API 호출: userId=$userId")
                val request = AddFriendRequest(userId = userId)
                val response = userApiService.sendFriendRequest(request)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "친구 요청 전송 성공")
                    true
                } else {
                    Log.e(TAG, "친구 요청 전송 실패: ${response.errorBody()?.string()}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "친구 요청 전송 중 오류 발생", e)
                false
            }
        }
    }

    // 친구 추가 API
    suspend fun addFriend(request: AddFriendRequest): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val token = preferencesUtil.getAccessToken()
                if (token == null) {
                    Log.e(TAG, "토큰이 없습니다")
                    return@withContext false
                }
                
                Log.d(TAG, "친구 추가 API 호출: userId=${request.userId}")
                val response = userApiService.addFriend(request)
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d(TAG, "프로필 이미지 업로드 응답: $responseBody")
                    if (responseBody?.isSuccess == true) {
                        Log.d(TAG, "친구 추가 성공")
                        return@withContext true
                    } else {
                        Log.e(TAG, "친구 추가 실패: ${responseBody?.message}")
                        return@withContext false
                    }
                } else {
                    Log.e(TAG, "친구 추가 API 호출 실패: ${response.code()} - ${response.errorBody()?.string()}")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "친구 추가 중 오류 발생", e)
                if (e is HttpException) {
                    Log.e(TAG, "HTTP 오류 코드: ${e.code()}")
                }
                return@withContext false
            }
        }
    }

    // 프로필 이미지 업로드 기능
    suspend fun uploadProfileImage(userId: Long, imageUri: Uri, fileName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val token = preferencesUtil.getAccessToken()
                if (token == null) {
                    Log.e(TAG, "토큰이 없습니다")
                    return@withContext false
                }

                // Uri 파일로 변환
                val file = uriToFile(imageUri, fileName)
                if (file == null) {
                    Log.e(TAG, "Uri를 파일로 변환하는데 실패했습니다")
                    return@withContext false
                }

                // MultipartBody.Part 생성 - 파라미터 이름을 'file'로 변경
                val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)

                // API 호출
                Log.d(TAG, "프로필 이미지 업로드 API 호출: userId=$userId, fileName=${file.name}")
                val response = userApiService.uploadProfileImage(filePart)

                // 임시 파일 삭제
                file.delete()

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d(TAG, "프로필 이미지 업로드 응답: $responseBody")
                    Log.d(TAG, "응답 원본 내용: ${response.raw()}")
                    
                    if (responseBody?.isSuccess == true) {
                        Log.d(TAG, "프로필 이미지 업로드 성공")
                        
                        // 응답의 result 필드에서 이미지 URL 추출 시도
                        try {
                            // result가 URL 문자열일 수 있으니 다양한 시도를 해봄
                            var imageUrl: String? = null
                            
                            // 1. 직접 toString() 시도
                            val resultString = responseBody.result?.toString()
                            if (!resultString.isNullOrEmpty() && !resultString.equals("null", ignoreCase = true)) {
                                imageUrl = resultString
                                Log.d(TAG, "1방법: result를 문자열로 변환 성공: $imageUrl")
                            } 
                            // 2. result가 Map이라고 가정하고 시도
                            else if (responseBody.result is Map<*,*>) {
                                val resultMap = responseBody.result as Map<*, *>
                                val urlFromMap = resultMap["url"] ?: resultMap["imageUrl"] ?: resultMap["profile_url"]
                                if (urlFromMap != null) {
                                    imageUrl = urlFromMap.toString()
                                    Log.d(TAG, "2방법: result가 Map이고 URL 키가 존재함: $imageUrl")
                                }
                            }
                            
                            // 이미지 URL이 유효하면 저장
                            if (!imageUrl.isNullOrEmpty()) {
                                Log.d(TAG, "추출된 이미지 URL: $imageUrl")
                                // 로컬에 URL 캐시 저장
                                preferencesUtil.setString("user_profile_image_url", imageUrl)
                                Log.d(TAG, "사용자 프로필 이미지 URL이 저장됨: $imageUrl")
                            } else {
                                // 업로드된 이미지를 무시하고 선택한 이미지 URI를 로컬에 저장
                                val selectedUriStr = imageUri.toString()
                                Log.d(TAG, "서버에서 URL을 제공하지 않음, 선택한 이미지 URI를 사용: $selectedUriStr")
                                preferencesUtil.setString("user_profile_image_uri", selectedUriStr)
                                preferencesUtil.setString("user_profile_image_url", selectedUriStr) // URL로도 동일하게 저장
                                Log.d(TAG, "사용자가 선택한 이미지 URI가 저장됨: $selectedUriStr")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "이미지 URL 추출 실패", e)
                            // 실패 시 선택한 이미지 URI 저장
                            val selectedUriStr = imageUri.toString()
                            Log.d(TAG, "예외 발생, 선택한 이미지 URI를 사용: $selectedUriStr")
                            preferencesUtil.setString("user_profile_image_uri", selectedUriStr)
                            preferencesUtil.setString("user_profile_image_url", selectedUriStr)
                            Log.d(TAG, "예외 발생 후 사용자가 선택한 이미지 URI가 저장됨: $selectedUriStr")
                        }
                        
                        return@withContext true
                    } else {
                        Log.e(TAG, "프로필 이미지 업로드 실패: ${responseBody?.message}")
                        return@withContext false
                    }
                } else {
                    Log.e(TAG, "프로필 이미지 업로드 API 호출 실패: ${response.code()} - ${response.errorBody()?.string()}")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "프로필 이미지 업로드 중 오류 발생", e)
                if (e is HttpException) {
                    Log.e(TAG, "HTTP 오류 코드: ${e.code()}")
                }
                return@withContext false
            }
        }
    }
    
    // 프로필 이미지 초기화 기능
    suspend fun resetProfileImage(userId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val token = preferencesUtil.getAccessToken()
                if (token == null) {
                    Log.e(TAG, "토큰이 없습니다")
                    return@withContext false
                }
                
                // API 호출
                Log.d(TAG, "프로필 이미지 초기화 API 호출")
                val response = userApiService.resetProfileImage()
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody?.isSuccess == true) {
                        Log.d(TAG, "프로필 이미지 초기화 성공")
                        
                        // 기본 이미지 URL을 로컬에 저장 - null 또는 빈 문자열로 설정
                        preferencesUtil.removeString("user_profile_image_url")
                        preferencesUtil.removeString("user_profile_image_uri")
                        Log.d(TAG, "로컬에 저장된 프로필 이미지 관련 데이터 모두 삭제")
                        
                        return@withContext true
                    } else {
                        Log.e(TAG, "프로필 이미지 초기화 실패: ${responseBody?.message}")
                        return@withContext false
                    }
                } else {
                    Log.e(TAG, "프로필 이미지 초기화 API 호출 실패: ${response.code()} - ${response.errorBody()?.string()}")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "프로필 이미지 초기화 중 오류 발생", e)
                if (e is HttpException) {
                    Log.e(TAG, "HTTP 오류 코드: ${e.code()}")
                }
                return@withContext false
            }
        }
    }

    // Uri를 임시 파일로 변환
    private fun uriToFile(uri: Uri, fileName: String): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, fileName)
            
            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Uri를 파일로 변환하는 중 오류 발생", e)
            null
        }
    }
}