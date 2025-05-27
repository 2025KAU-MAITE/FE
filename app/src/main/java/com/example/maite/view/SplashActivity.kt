package com.example.maite.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import com.example.maite.PreferencesUtil
import com.example.maite.R
import com.example.maite.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    
    private val TAG = "SplashActivity"
    private lateinit var binding: ActivitySplashBinding
    private lateinit var preferencesUtil: PreferencesUtil
    
    // "Hi Maite!" 이미지에서 "Maite" 텍스트 위치 조정 상수
    // 실제 테스트를 통해 이 값을 미세 조정할 수 있습니다
    private val MAITE_TEXT_OFFSET_RATIO = 0.68f // 이미지 높이의 68% 지점
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 시스템 스플래시를 완전히 비활성화했으므로 바로 커스텀 레이아웃 표시
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesUtil = PreferencesUtil(this)
        
        // 레이아웃이 완료되면 즉시 애니메이션 시작
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                startLogoAnimation()
            }
        })
    }
    
    private fun startLogoAnimation() {
        // 로고를 잠시 중앙에서 보여준 후 애니메이션 시작
        Handler(Looper.getMainLooper()).postDelayed({
            animateLogoToLoginPosition()
        }, 1200) // 1.2초 동안 중앙에서 보여줌 (충분히 보여주기)
    }
    
    private fun animateLogoToLoginPosition() {
        // 정확한 목표 위치 계산
        val targetPosition = calculateExactTargetPosition()
        
        Log.d(TAG, "애니메이션 시작 - 현재 위치에서 목표 위치로 이동: $targetPosition")
        
        // Y축 이동 애니메이션
        val translateAnimator = ObjectAnimator.ofFloat(
            binding.ivSplashLogo, 
            "translationY", 
            0f, 
            targetPosition.y
        ).apply {
            duration = 800 // 0.8초 동안 이동 (부드럽게)
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // 애니메이션 완료 후 즉시 로그인 화면으로 전환 (중간 흰 화면 없이)
        translateAnimator.doOnEnd {
            Log.d(TAG, "애니메이션 완료 - 즉시 로그인 화면으로 전환")
            navigateToLoginImmediately()
        }
        
        translateAnimator.start()
    }
    
    private fun calculateExactTargetPosition(): TargetPosition {
        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        
        // 상태바 높이
        val statusBarHeight = getStatusBarHeight()
        
        // 로그인 화면의 정확한 구조:
        // - 상태바
        // - loginUIContainer padding: 24dp
        // - ivLogo marginTop: 50dp
        val containerPadding = (24 * density).toInt()
        val logoMarginTop = (50 * density).toInt()
        
        // 현재 스플래시 로고의 실제 위치 (화면 중앙)
        val splashLogoLocation = IntArray(2)
        binding.ivSplashLogo.getLocationOnScreen(splashLogoLocation)
        val currentLogoY = splashLogoLocation[1] + binding.ivSplashLogo.height / 2f // 로고의 중앙 Y 위치
        
        // 로그인 화면에서 hi_maite 이미지가 위치할 절대 Y 좌표 (top)
        val targetAbsoluteY = statusBarHeight + containerPadding + logoMarginTop
        
        // hi_maite 이미지의 실제 크기 (픽셀 단위)
        val hiMaiteDrawable = resources.getDrawable(R.drawable.hi_maite, null)
        val hiMaiteHeight = hiMaiteDrawable?.intrinsicHeight ?: 120
        
        // "Hi Maite!" 이미지에서 "Maite" 부분의 위치 추정
        // 일반적으로 "Hi"는 위쪽에, "Maite!"는 아래쪽에 위치할 것으로 예상
        val targetMaiteTextY = targetAbsoluteY + (hiMaiteHeight * MAITE_TEXT_OFFSET_RATIO)
        
        // 이동해야 할 상대적 거리 (스플래시 로고 중앙 → "Maite" 텍스트 위치)
        val relativeY = targetMaiteTextY - currentLogoY
        
        Log.d(TAG, "위치 계산 - 상태바: $statusBarHeight, 컨테이너 패딩: $containerPadding, 로고 마진: $logoMarginTop")
        Log.d(TAG, "위치 계산 - 현재 로고 중앙 Y: $currentLogoY")
        Log.d(TAG, "위치 계산 - hi_maite 높이: $hiMaiteHeight")
        Log.d(TAG, "위치 계산 - 목표 절대위치: $targetAbsoluteY, Maite 텍스트 Y: $targetMaiteTextY")
        Log.d(TAG, "위치 계산 - 이동 거리: $relativeY")
        
        return TargetPosition(0f, relativeY)
    }
    
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
    
    private fun navigateToLoginImmediately() {
        // 로고를 정확한 위치에 고정 (LoginActivity가 시작될 때까지)
        binding.ivSplashLogo.clearAnimation()
        
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("from_splash", true)
        
        // SINGLE_TOP으로 더 빠른 전환
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        // 즉시 Activity 시작 
        startActivity(intent)
        
        // 애니메이션 완전히 제거 - 즉시 전환
        overridePendingTransition(0, 0)
        
        // 즉시 finish
        finish()
        overridePendingTransition(0, 0)
    }
    
    data class TargetPosition(val x: Float, val y: Float)
}
