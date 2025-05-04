package com.example.maite // 실제 프로젝트의 Application 클래스 패키지 경로

import android.app.Application
import com.example.maite.util.AuthTokenManager // AuthTokenManager import

// 클래스 이름을 MaiteApplication으로 변경
class MaiteApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // 앱이 시작될 때 AuthTokenManager 초기화
        AuthTokenManager.init(applicationContext)
    }
}