package com.example.maite.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.maite.model.LoginRequest
import com.example.maite.model.LoginResponse
import com.example.maite.model.User

class LoginViewModel : ViewModel() {
    
    private val _loginResult = MutableLiveData<LoginResponse>()
    val loginResult: LiveData<LoginResponse> = _loginResult
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    fun login(email: String, password: String) {
        // Set loading state
        _isLoading.value = true
        
        // In a real app, this would call a repository or service to authenticate
        // This is a mock implementation
        if (email.isNotEmpty() && password.isNotEmpty()) {
            // Simulate network delay
            android.os.Handler().postDelayed({
                val mockUser = User(
                    userId = "123456",
                    email = email,
                    name = "Sample User",
                    token = "sample_jwt_token"
                )
                _loginResult.value = LoginResponse(
                    success = true,
                    message = "로그인 성공",
                    user = mockUser
                )
                _isLoading.value = false
            }, 1000)
        } else {
            _loginResult.value = LoginResponse(
                success = false,
                message = "이메일과 비밀번호를 입력해주세요.",
                user = null
            )
            _isLoading.value = false
        }
    }
    
    fun resetLoginState() {
        _loginResult.value = null
        _isLoading.value = false
    }
}