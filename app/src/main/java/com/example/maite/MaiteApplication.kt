package com.example.maite

import android.app.Application
import com.example.maite.network.WebSocketManager

class MaiteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UserManager.init(this)

        val preferencesUtil = PreferencesUtil(applicationContext)
        WebSocketManager.getInstance().initialize(preferencesUtil)

        if (!preferencesUtil.getAccessToken().isNullOrEmpty()) {
            WebSocketManager.getInstance().connect()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        WebSocketManager.getInstance().disconnect()
    }
}