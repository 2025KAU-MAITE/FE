package com.example.maite.view

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.maite.PreferencesUtil
import com.example.maite.R
import com.example.maite.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySplashBinding
    private lateinit var preferencesUtil: PreferencesUtil
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesUtil = PreferencesUtil(this)
        
        // 즉시 전환 - 애니메이션 없이 바로 로그인 화면으로
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToLoginImmediately()
        }, 1000) // 1초 동안만 스플래시 표시
    }
    
    private fun navigateToLoginImmediately() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        startActivity(intent)
        // 안드로이드 기본 부드러운 페이드 전환 애니메이션 사용
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        
        finish()
    }
}
