package com.example.maite

import android.content.Context

object UserManager {
    private var preferencesUtil: PreferencesUtil? = null

    fun init(context: Context) {
        if (preferencesUtil == null) {
            preferencesUtil = PreferencesUtil(context.applicationContext)
        }
    }

    fun setUserInfo(userId: Long, name: String, email: String) {
        preferencesUtil?.saveUserInfo(userId, name, email)
    }

    fun getUserId(): Long? {
        return preferencesUtil?.getUserId()
    }

    fun getUserName(): String? {
        return preferencesUtil?.getUserName()  // PreferencesUtil에 추가 필요
    }

    fun getUserEmail(): String? {
        return preferencesUtil?.getUserEmail()  // PreferencesUtil에 추가 필요
    }

    fun isLoggedIn(): Boolean {
        return preferencesUtil?.getAccessToken() != null
    }

    fun clearUserData() {
        preferencesUtil?.clearAllData()
    }
}