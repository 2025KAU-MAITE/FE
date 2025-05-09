package com.example.maite.ui.notification

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.maite.data.api.NotificationApiService
import com.example.maite.data.network.RetrofitClient
import com.example.maite.data.repository.NotificationRepository

class NotificationViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            // RetrofitClient를 사용하여 새로운 Retrofit 인스턴스 생성
            val retrofitInstance = RetrofitClient.getRetrofit(context)
            val apiService = retrofitInstance.create(NotificationApiService::class.java)
            val repository = NotificationRepository(apiService)
            
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
