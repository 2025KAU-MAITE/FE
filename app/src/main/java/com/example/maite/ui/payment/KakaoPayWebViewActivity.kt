package com.example.maite.ui.payment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
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
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent 호출 - 카카오톡에서 돌아옴")
        
        // 카카오톡에서 돌아온 경우의 처리
        intent?.data?.let { uri ->
            val url = uri.toString()
            Log.d(TAG, "Intent로 받은 URL: $url")
            
            // 결제 결과 URL인지 확인하고 처리
            if (handleUrlRedirect(url)) {
                return
            }
        }
        
        // Intent URL이 없으면 웹뷰를 새로고침하여 결제 상태 확인
        Log.d(TAG, "웹뷰 새로고침하여 결제 상태 확인")
        binding.webView.reload()
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume 호출")
        
        // 카카오톡에서 돌아온 후 결제 상태를 확인하기 위해 웹뷰 새로고침
        // 단, onCreate 직후가 아닌 경우에만
        if (::binding.isInitialized && binding.webView.url != null) {
            Log.d(TAG, "카카오톡에서 돌아온 후 결제 상태 확인을 위해 새로고침")
            // 짧은 지연 후 새로고침 (카카오톡 전환 애니메이션 완료 후)
            binding.webView.postDelayed({
                binding.webView.reload()
            }, 500)
        }
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
        binding.btnBack.setOnClickListener {
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
            
            // User Agent 설정 (카카오페이가 모바일 결제로 인식하도록)
            val originalUA = settings.userAgentString
            settings.userAgentString = "$originalUA MAITE_APP Android Mobile"
            
            Log.d(TAG, "WebView User-Agent: ${settings.userAgentString}")
            
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
                
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    
                    val errorCode = error?.errorCode ?: -1
                    val description = error?.description?.toString() ?: "Unknown error"
                    val failingUrl = request?.url?.toString() ?: "Unknown URL"
                    
                    Log.e(TAG, "WebView 에러 발생: errorCode=$errorCode, description=$description, failingUrl=$failingUrl")
                    
                    // 카카오톡 앱 호출 실패 시 (에뮬레이터 등) - 에러 로그만 남기고 계속 진행
                    if (failingUrl.startsWith("intent://") && errorCode == ERROR_UNKNOWN_URL_SCHEME) {
                        Log.w(TAG, "카카오톡 앱이 설치되지 않았거나 에뮬레이터 환경입니다. 웹 결제로 계속 진행합니다.")
                        // 취소하지 않고 계속 진행
                    }
                }
                
                @Deprecated("Deprecated in Java")
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e(TAG, "WebView 에러 발생 (Legacy): errorCode=$errorCode, description=$description, failingUrl=$failingUrl")
                    
                    // 카카오톡 앱 호출 실패 시 (에뮬레이터 등) - 에러 로그만 남기고 계속 진행
                    if (failingUrl?.startsWith("intent://") == true && errorCode == ERROR_UNKNOWN_URL_SCHEME) {
                        Log.w(TAG, "카카오톡 앱이 설치되지 않았거나 에뮬레이터 환경입니다. 웹 결제로 계속 진행합니다.")
                        // 취소하지 않고 계속 진행
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
        
        // 카카오톡 앱 호출 URL 처리
        if (url.startsWith("intent://") && url.contains("kakaotalk")) {
            return handleKakaoTalkAppLaunch(url)
        }
        
        // 다른 앱 호출 URL 처리 (카카오페이 앱 등)
        if (url.startsWith("intent://")) {
            return handleIntentUrl(url)
        }
        
        // 커스텀 스킴 URL 처리
        if (url.startsWith("kakaotalk://") || url.startsWith("kakaokompassauth://")) {
            return handleCustomScheme(url)
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
     * 카카오톡 앱 실행 처리
     */
    private fun handleKakaoTalkAppLaunch(intentUrl: String): Boolean {
        return try {
            Log.d(TAG, "카카오톡 앱 실행 시도: $intentUrl")
            
            // Intent URL 파싱
            val intent = Intent.parseUri(intentUrl, Intent.URI_INTENT_SCHEME)
            
            // 카카오톡 앱이 설치되어 있는지 확인
            if (isAppInstalled(intent.`package`)) {
                Log.d(TAG, "카카오톡 앱이 설치되어 있음. 앱으로 이동")
                startActivity(intent)
                true
            } else {
                Log.w(TAG, "카카오톡 앱이 설치되지 않음. 웹으로 계속 진행")
                // 앱이 없으면 웹에서 계속 진행
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "카카오톡 앱 실행 실패. 웹으로 계속 진행: ${e.message}")
            false
        }
    }
    
    /**
     * Intent URL 처리 (일반적인 앱 호출)
     */
    private fun handleIntentUrl(intentUrl: String): Boolean {
        return try {
            Log.d(TAG, "Intent URL 처리: $intentUrl")
            
            val intent = Intent.parseUri(intentUrl, Intent.URI_INTENT_SCHEME)
            
            // 앱이 설치되어 있는지 확인
            if (canHandleIntent(intent)) {
                Log.d(TAG, "앱이 설치되어 있음. 앱으로 이동")
                startActivity(intent)
                true
            } else {
                Log.w(TAG, "앱이 설치되지 않음. 대체 URL 확인")
                
                // 대체 URL이 있으면 사용
                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                if (!fallbackUrl.isNullOrEmpty()) {
                    Log.d(TAG, "대체 URL로 이동: $fallbackUrl")
                    binding.webView.loadUrl(fallbackUrl)
                    return true
                }
                
                // 마켓 URL이 있으면 사용 (앱 설치 페이지)
                val marketUrl = intent.getStringExtra("market_url")
                if (!marketUrl.isNullOrEmpty()) {
                    Log.d(TAG, "마켓 URL로 이동: $marketUrl")
                    val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse(marketUrl))
                    if (canHandleIntent(marketIntent)) {
                        startActivity(marketIntent)
                        return true
                    }
                }
                
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Intent URL 처리 실패: ${e.message}")
            false
        }
    }
    
    /**
     * 커스텀 스킴 URL 처리
     */
    private fun handleCustomScheme(url: String): Boolean {
        return try {
            Log.d(TAG, "커스텀 스킴 URL 처리: $url")
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            
            if (canHandleIntent(intent)) {
                Log.d(TAG, "커스텀 스킴으로 앱 실행")
                startActivity(intent)
                true
            } else {
                Log.w(TAG, "커스텀 스킴을 처리할 앱이 없음")
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "커스텀 스킴 URL 처리 실패: ${e.message}")
            false
        }
    }
    
    /**
     * 앱이 설치되어 있는지 확인
     */
    private fun isAppInstalled(packageName: String?): Boolean {
        return try {
            if (packageName.isNullOrEmpty()) return false
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Intent를 처리할 수 있는 앱이 있는지 확인
     */
    private fun canHandleIntent(intent: Intent): Boolean {
        return try {
            val activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            activities.isNotEmpty()
        } catch (e: Exception) {
            false
        }
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
