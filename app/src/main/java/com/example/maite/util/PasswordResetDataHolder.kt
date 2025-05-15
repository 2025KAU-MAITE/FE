package com.example.maite.util

import android.util.Log

/**
 * 비밀번호 재설정 관련 데이터를 저장하는 싱글톤 클래스
 */
object PasswordResetDataHolder {
    private const val TAG = "PasswordResetDataHolder"
    
    // 이메일 저장 (메모리)
    private var email: String? = null
    
    // 이메일 백업 (정적)
    private var backupEmail: String? = null
    
    /**
     * 이메일 설정
     */
    fun setEmail(userEmail: String) {
        Log.d(TAG, "이메일 저장: '$userEmail'")
        email = userEmail
        backupEmail = userEmail // 정적 백업도 함께 저장
    }
    
    /**
     * 이메일 가져오기
     */
    fun getEmail(): String? {
        // 메모리에서 먼저 시도, 없으면 정적 백업에서 시도
        val result = email ?: backupEmail
        Log.d(TAG, "이메일 조회 결과: '$result', (메모리: '$email', 백업: '$backupEmail')")
        return result
    }
    
    /**
     * 데이터 초기화
     */
    fun clear() {
        Log.d(TAG, "이메일 데이터 초기화 (기존값: '$email')")
        email = null
        // 백업 이메일은 유지 (안전망)
    }
}
