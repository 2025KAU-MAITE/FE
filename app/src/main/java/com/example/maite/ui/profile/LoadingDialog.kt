package com.example.maite.ui.profile

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.view.Window
import android.view.WindowManager

/**
 * 로딩 다이얼로그 클래스
 * 데이터 저장, 로드 등 API 호출 시 사용자에게 진행 중임을 알리는 다이얼로그
 */
class LoadingDialog(context: Context) : Dialog(context) {
    
    // 다이얼로그 표시 여부를 확인하는 속성 추가
    var isDialogShowing: Boolean = false
        private set
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 다이얼로그 배경 및 창 설정
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(false)
        
        // 프로그레스바를 다이얼로그 콘텐츠로 설정
        val progressBar = ProgressBar(context)
        val layout = RelativeLayout(context)
        val params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.CENTER_IN_PARENT)
        layout.addView(progressBar, params)
        
        // 레이아웃을 다이얼로그 콘텐츠로 설정
        setContentView(layout)
        
        // 다이얼로그 크기 설정
        val displayMetrics = context.resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.7).toInt()
        window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
    }
    
    // show() 메소드 오버라이드하여 상태 변경
    override fun show() {
        super.show()
        isDialogShowing = true
    }
    
    // dismiss() 메소드 오버라이드하여 상태 변경
    override fun dismiss() {
        super.dismiss()
        isDialogShowing = false
    }
}