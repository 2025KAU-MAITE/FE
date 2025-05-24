package com.example.maite.ui.payment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.maite.R
import com.example.maite.databinding.ActivityKakaoPayWebViewBinding

/**
 * 카카오페이 결제를 위한 WebView 액티비티
 */
class KakaoPayWebViewActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityKakaoPayWebViewBinding
    private val TAG = "KakaoPayWebView"
    
    companion object {
        const val EXTRA_PAYMENT_URL = "payment_url"
        const val RESULT_SUCCESS = 100
        const val RESULT_CANCEL = 101
        const val RESULT_FAIL = 102
        
        const val EXTRA_PG_TOKEN = "pg_token"
        const val EXTRA_ERROR_MESSAGE = "error_message"
        
        // 콜백 URL 패턴
        private const val SUCCESS_URL_PREFIX = "http://3.39.205.32/kakao/success"
        private const val CANCEL_URL_PREFIX = "http://3.39.205.32/kakao/cancel"
        private const val FAIL_URL_PREFIX = "http://3.39.205.32/kakao/fail"
        
        // WebView 에러 코드
        private const val ERROR_UNKNOWN_URL_SCHEME = -10
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKakaoPayWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupBackPressedCallback()
        setupToolbar()
        setupWebView()
        
        val paymentUrl = intent.getStringExtra(EXTRA_PAYMENT_URL)
        if (paymentUrl.isNullOrEmpty()) {
            Log.e(TAG, "결제 URL이 없습니다")
            finishWithError("결제 URL이 없습니다")
            return
        }
        
        Log.d(TAG, "카카오페이 결제 URL 로드: $paymentUrl")
        binding.webView.loadUrl(paymentUrl)
    }
    
    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    finishWithCancel()
                }
            }
        })
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "카카오페이 결제"
        }
        
        binding.toolbar.setNavigationOnClickListener {
            finishWithCancel()
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            
            // 카카오페이 결제를 위한 추가 설정
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            
            // User Agent 설정 (모바일 브라우저로 인식되도록)
            settings.userAgentString = settings.userAgentString + " MAITE_APP"
            
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    Log.d(TAG, "페이지 로드 시작: $url")
                    binding.progressBar.visibility = View.VISIBLE
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "페이지 로드 완료: $url")
                    binding.progressBar.visibility = View.GONE
                }
                
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e(TAG, "WebView 에러 발생: errorCode=$errorCode, description=$description, failingUrl=$failingUrl")
                    
                    // 카카오톡 앱 호출 실패 시 (에뮬레이터 등)
                    if (failingUrl?.startsWith("intent://") == true && errorCode == ERROR_UNKNOWN_URL_SCHEME) {
                        Log.w(TAG, "카카오톡 앱이 설치되지 않았거나 에뮬레이터 환경입니다. 웹 결제로 진행합니다.")
                        // 웹 버전으로 리다이렉트하거나 사용자에게 안내
                        handleKakaoTalkAppNotAvailable()
                    }
                }
                
                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    Log.e(TAG, "HTTP 에러 발생: ${request?.url}, statusCode=${errorResponse?.statusCode}")
                }
                
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    url?.let { 
                        Log.d(TAG, "URL 리디렉션: $it")
                        return handleUrlRedirect(it)
                    }
                    return false
                }
                
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    request?.url?.toString()?.let { url ->
                        Log.d(TAG, "URL 리디렉션 (API 24+): $url")
                        return handleUrlRedirect(url)
                    }
                    return false
                }
            }
            
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    binding.progressBar.progress = newProgress
                    
                    if (newProgress == 100) {
                        binding.progressBar.visibility = View.GONE
                    } else {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
            }
        }
    }
    
    /**
     * URL 리디렉션 처리
     * @param url 리디렉션된 URL
     * @return true if handled, false otherwise
     */
    private fun handleUrlRedirect(url: String): Boolean {
        Log.d(TAG, "URL 리디렉션 처리: $url")
        
        // 카카오톡 앱 호출 URL인 경우 특별 처리
        if (url.startsWith("intent://") && url.contains("kakaotalk")) {
            Log.w(TAG, "카카오톡 앱 호출 URL 감지: $url")
            handleKakaoTalkAppNotAvailable()
            return true
        }
        
        when {
            url.startsWith(SUCCESS_URL_PREFIX) -> {
                // 결제 성공
                val pgToken = extractPgTokenFromUrl(url)
                if (pgToken.isNotEmpty()) {
                    Log.d(TAG, "결제 성공 - pg_token: ${pgToken.take(10)}...")
                    finishWithSuccess(pgToken)
                } else {
                    Log.e(TAG, "pg_token을 찾을 수 없습니다: $url")
                    finishWithError("결제 정보를 확인할 수 없습니다")
                }
                return true
            }
            
            url.startsWith(CANCEL_URL_PREFIX) -> {
                // 결제 취소
                Log.d(TAG, "결제 취소")
                finishWithCancel()
                return true
            }
            
            url.startsWith(FAIL_URL_PREFIX) -> {
                // 결제 실패
                Log.d(TAG, "결제 실패")
                finishWithError("결제가 실패했습니다")
                return true
            }
        }
        
        return false
    }
    
    /**
     * URL에서 pg_token 추출
     * @param url 콜백 URL
     * @return pg_token 값
     */
    private fun extractPgTokenFromUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            uri.getQueryParameter("pg_token") ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "pg_token 추출 실패: ${e.message}", e)
            ""
        }
    }
    
    /**
     * 카카오톡 앱을 사용할 수 없을 때 처리
     */
    private fun handleKakaoTalkAppNotAvailable() {
        Log.w(TAG, "카카오톡 앱 사용 불가 - 에뮬레이터 또는 앱 미설치")
        
        // 사용자에게 안내 메시지 표시
        runOnUiThread {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("안내")
                .setMessage("에뮬레이터에서는 카카오톡 앱 결제를 지원하지 않습니다.\n\n실제 기기에서 테스트해주세요.\n\n(개발용: 결제 성공으로 처리하시겠습니까?)")
                .setPositiveButton("성공 처리") { _, _ ->
                    // 개발/테스트용으로 임시 성공 처리
                    finishWithSuccess("test_pg_token_for_emulator")
                }
                .setNegativeButton("취소") { _, _ ->
                    finishWithCancel()
                }
                .setCancelable(false)
                .show()
        }
    }
    
    /**
     * 결제 성공으로 종료
     */
    private fun finishWithSuccess(pgToken: String) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_PG_TOKEN, pgToken)
        }
        setResult(RESULT_SUCCESS, resultIntent)
        finish()
    }
    
    /**
     * 결제 취소로 종료
     */
    private fun finishWithCancel() {
        setResult(RESULT_CANCEL)
        finish()
    }
    
    /**
     * 결제 실패로 종료
     */
    private fun finishWithError(errorMessage: String) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
        }
        setResult(RESULT_FAIL, resultIntent)
        finish()
    }
}
