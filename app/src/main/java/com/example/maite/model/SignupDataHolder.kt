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
    
    // 소셜 로그인 관련 정보
    var provider: String = "" // "GOOGLE"
    var idToken: String = "" // 소셜 로그인에서 받은 ID 토큰
    var accessToken: String = "" // 소셜 로그인에서 받은 액세스 토큰
    
    /**
     * 이메일 및 비밀번호 저장
     */
    fun saveAccountInfo(email: String, password: String) {
        this.email = email
        this.password = password
    }
    
    /**
     * 소셜 로그인 정보 저장
     */
    fun saveSocialLoginInfo(email: String, provider: String, idToken: String, accessToken: String = "") {
        this.email = email
        this.provider = provider
        this.idToken = idToken
        this.accessToken = accessToken
        this.password = "" // 소셜 로그인은 비밀번호가 필요 없음
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
        provider = ""
        idToken = ""
        accessToken = ""
    }
}