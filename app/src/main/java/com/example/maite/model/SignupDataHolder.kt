package com.example.maite.model

/**
 * 회원가입 과정에서 수집된 데이터를 저장하는 싱글톤 클래스
 */
object SignupDataHolder {
    // 기본 계정 정보
    var email: String = ""
    var password: String = ""
    
    // 추가 프로필 정보 (필요에 따라 확장 가능)
    var name: String = ""
    var phoneNumber: String = ""
    var address: String = ""
    var profileImageUrl: String = ""
    
    /**
     * 이메일 및 비밀번호 저장
     */
    fun saveAccountInfo(email: String, password: String) {
        this.email = email
        this.password = password
    }
    
    /**
     * 모든 데이터 초기화
     */
    fun clear() {
        email = ""
        password = ""
        name = ""
        phoneNumber = ""
        address = ""
        profileImageUrl = ""
    }
}