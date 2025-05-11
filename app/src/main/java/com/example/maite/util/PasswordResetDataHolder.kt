package com.example.maite.util

/**
 * 비밀번호 재설정 관련 데이터를 저장하는 싱글톤 클래스
 */
object PasswordResetDataHolder {
    private var email: String? = null
    
    /**
     * 이메일 설정
     */
    fun setEmail(userEmail: String) {
        email = userEmail
    }
    
    /**
     * 이메일 가져오기
     */
    fun getEmail(): String? {
        return email
    }
    
    /**
     * 데이터 초기화
     */
    fun clear() {
        email = null
    }
}
