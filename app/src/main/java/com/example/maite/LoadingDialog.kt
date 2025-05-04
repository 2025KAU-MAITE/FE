package com.example.maite

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout // FrameLayout 추가
import com.example.maite.databinding.LoadingDialogBinding

class LoadingDialog(private val context: Context) {

    // contentViewBinding으로 이름 변경
    private lateinit var contentViewBinding: LoadingDialogBinding
    private val handler = Handler(Looper.getMainLooper())
    private var currentDots = 0
    private val loadingTexts = arrayOf("로딩중", "로딩중.", "로딩중..", "로딩중...")

    // 배경 뷰와 콘텐츠 뷰 분리
    private var backgroundView: View? = null
    private var contentView: View? = null
    private var isShowing = false

    private val updateLoadingText = object : Runnable {
        override fun run() {
            if (isShowing && ::contentViewBinding.isInitialized) { // contentViewBinding 사용
                contentViewBinding.loadingTextView.text = loadingTexts[currentDots]
                currentDots = (currentDots + 1) % loadingTexts.size
                handler.postDelayed(this, 500)
            }
        }
    }

    fun show() {
        if (isShowing) return
        val activity = context as? Activity ?: return
        val decorView = activity.window.decorView as ViewGroup
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)

        // 뷰들이 null이면 처음 생성
        if (backgroundView == null || contentView == null) {
            // 1. 배경 뷰 생성 및 설정
            backgroundView = FrameLayout(context).apply { // FrameLayout으로 배경 뷰 생성
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.parseColor("#99000000")) // 반투명 배경 설정
                isClickable = true // 클릭 이벤트 가로채기
                isFocusable = true
            }

            // 2. 콘텐츠 뷰 생성 (기존 binding 로직 활용)
            contentViewBinding = LoadingDialogBinding.inflate(LayoutInflater.from(context))
            contentView = contentViewBinding.root
            // 중요: 콘텐츠 뷰 자체에는 배경색을 설정하지 않음!
        }

        // 이전 뷰 제거 (메모리 누수 방지)
        (backgroundView?.parent as? ViewGroup)?.removeView(backgroundView)
        (contentView?.parent as? ViewGroup)?.removeView(contentView)

        // 루트 뷰에 배경 뷰와 콘텐츠 뷰 순서대로 추가
        rootView.addView(backgroundView)
        rootView.addView(contentView) // 콘텐츠 뷰를 배경 뷰 위에 추가
        isShowing = true

        currentDots = 0
        startLoadingAnimation()
    }

    private fun startLoadingAnimation() {
        handler.removeCallbacks(updateLoadingText)
        handler.post(updateLoadingText)
    }

    fun dismiss() {
        if (!isShowing) return
        handler.removeCallbacks(updateLoadingText)
        val activity = context as? Activity ?: return
        val decorView = activity.window.decorView as ViewGroup
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)

        // 루트 뷰에서 배경 뷰와 콘텐츠 뷰 제거
        rootView.removeView(backgroundView)
        rootView.removeView(contentView)

        isShowing = false
        // 필요에 따라 뷰 재사용 안 할 경우 null 설정
        // backgroundView = null
        // contentView = null
    }

    val isDialogShowing: Boolean
        get() = isShowing
}