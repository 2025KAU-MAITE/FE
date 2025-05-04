package com.example.maite

import android.app.Application

class MaiteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UserManager.init(this)
    }
}