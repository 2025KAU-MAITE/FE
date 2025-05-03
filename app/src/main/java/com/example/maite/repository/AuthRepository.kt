package com.example.maite.repository

import com.example.maite.ApiClient
import com.example.maite.model.AuthApi
import com.example.maite.model.EmailCheckRequest
import com.example.maite.model.EmailCheckResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {
    private val authApi = ApiClient.retrofit.create(AuthApi::class.java)
    
    suspend fun checkEmailDuplicate(email: String): EmailCheckResponse {
        return withContext(Dispatchers.IO) {
            authApi.checkEmailDuplicate(EmailCheckRequest(email))
        }
    }
}