package com.example.maite

import android.content.Context
import android.content.SharedPreferences

class PreferencesUtil(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("maite_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_LAST_JOINED_ROOM_ID = "last_joined_room_id"
    }

    fun saveAccessToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun clearAccessToken() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).apply()
    }

    fun saveUserId(userId: Long) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): Long? {
        return if (prefs.contains(KEY_USER_ID)) {
            prefs.getLong(KEY_USER_ID, -1L)
        } else {
            null
        }
    }

    fun saveUserInfo(userId: Long, name: String, email: String) {
        prefs.edit().apply {
            putLong(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            apply()
        }
    }

    fun clearAllData() {
        prefs.edit().clear().apply()
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun saveLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long = -1L): Long? {
        return if (prefs.contains(key)) {
            prefs.getLong(key, defaultValue)
        } else {
            null
        }
    }

    /**
     * 마지막으로 참가한 회의방 ID 저장
     */
    fun setLastJoinedRoomId(roomId: Int) {
        prefs.edit().putInt(KEY_LAST_JOINED_ROOM_ID, roomId).apply()
    }

    /**
     * 마지막으로 참가한 회의방 ID 가져오기
     * @return null 값은 저장된 값이 없을 때 반환
     */
    fun getLastJoinedRoomId(): Int? {
        return if (prefs.contains(KEY_LAST_JOINED_ROOM_ID)) {
            prefs.getInt(KEY_LAST_JOINED_ROOM_ID, -1)
        } else {
            null
        }
    }

    /**
     * 마지막으로 참가한 회의방 ID 초기화 
     * 이미 처리된 회의방 ID를 지워서 중복 처리를 방지함
     */
    fun clearLastJoinedRoomId() {
        prefs.edit().remove(KEY_LAST_JOINED_ROOM_ID).apply()
    }
}