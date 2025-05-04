package com.example.maite

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.example.maite.databinding.LoadingDialogBinding

class LoadingDialog(private val context: Context) {
    private lateinit var binding: LoadingDialogBinding
    private val handler = Handler(Looper.getMainLooper())
    private var currentDots = 0
    private val loadingTexts = arrayOf("로딩중", "로딩중.", "로딩중..", "로딩중...")
    private var loadingView: View? = null
    private var isShowing = false

    private val updateLoadingText = object : Runnable {
        override fun run() {
            if (isShowing) {  // 로딩 뷰가 표시 중일 때만 실행
                binding.loadingTextView.text = loadingTexts[currentDots]
                currentDots = (currentDots + 1) % loadingTexts.size
                handler.postDelayed(this, 500)
            }
        }
    }

    fun show() {
        if (isShowing) return

        val activity = context as FragmentActivity
        val rootView = activity.findViewById<FrameLayout>(R.id.main_frm)

        if (loadingView == null) {
            binding = LoadingDialogBinding.inflate(LayoutInflater.from(context))
            loadingView = binding.root

            // 배경을 반투명하게 설정
            binding.root.setBackgroundColor(Color.parseColor("#99000000")) // 반투명 배경
        }

        // 이미 부모 뷰가 있다면 제거
        (loadingView?.parent as? ViewGroup)?.removeView(loadingView)

        // main_frm에 로딩 뷰 추가 (bottomNavigationView는 가리지 않음)
        rootView.addView(loadingView)
        isShowing = true

        // 애니메이션 시작
        currentDots = 0
        startLoadingAnimation()
    }

    private fun startLoadingAnimation() {
        handler.removeCallbacks(updateLoadingText)  // 기존 콜백 제거
        handler.post(updateLoadingText)  // 새로운 애니메이션 시작
    }

    fun dismiss() {
        if (!isShowing) return

        handler.removeCallbacks(updateLoadingText)

        val activity = context as FragmentActivity
        val rootView = activity.findViewById<FrameLayout>(R.id.main_frm)
        rootView.removeView(loadingView)

        isShowing = false
    }

    // 기존 메소드는 호환성을 위해 빈 구현으로 유지
    fun showBottomSheetBackground() {
        // 빈 구현
    }

    fun hideBottomSheetBackground() {
        // 빈 구현
    }

    val isDialogShowing: Boolean
        get() = isShowing
}