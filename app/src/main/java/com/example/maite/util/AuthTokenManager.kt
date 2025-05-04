package com.example.maite.util // 실제 프로젝트의 패키지 경로에 맞게 수정하세요

import android.content.Context
import android.content.SharedPreferences

object AuthTokenManager {

    // LoginActivity에서 사용하는 SharedPreferences 설정과 일치시킵니다.
    private const val PREF_NAME = "maite_prefs"
    private const val KEY_AUTH_TOKEN = "access_token"

    private lateinit var sharedPreferences: SharedPreferences

    // Application 클래스에서 앱 시작 시 호출하여 초기화합니다.
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // 토큰을 SharedPreferences에 저장합니다.
    fun saveToken(token: String?) {
        sharedPreferences.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    // 저장된 토큰을 SharedPreferences에서 검색합니다.
    fun getToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }

    // 로그아웃 등 필요시 토큰을 삭제합니다.
    fun clearToken() {
        sharedPreferences.edit().remove(KEY_AUTH_TOKEN).apply()
    }
}