package com.example.maite.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.maite.databinding.FragmentSignupAddressBinding
import com.example.maite.model.SignupDataHolder

class SignupAddressFragment : Fragment() {

    private val TAG = "SignupAddressFragment"
    private var _binding: FragmentSignupAddressBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupAddressBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupWebView()
        setupListeners()
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // WebView 설정
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            useWideViewPort = true  // HTML 컨텐츠가 웹뷰에 맞게 표시됨
            loadWithOverviewMode = true  // 화면에 맞게 크기 조정
            setSupportZoom(true)   // 확대/축소 지원
            builtInZoomControls = true // 빌트인 확대/축소 컨트롤 활성화
            displayZoomControls = false // 화면에 확대/축소 컨트롤 표시 안함
            setGeolocationEnabled(true) // 위치 정보 사용 허용
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE // 캐시 사용 중지
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW // 혼합 콘텐츠 허용
            userAgentString = userAgentString + " maiteDaumAddressApp" // 사용자 지정 에이전트 추가
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
        }
        
        // JavaScript 인터페이스 추가 - 중요: "Android" 이름은 HTML에서의 호출명과 일치해야 함
        val webInterface = WebViewInterface()
        binding.webView.addJavascriptInterface(webInterface, "Android")
        Log.d(TAG, "JavaScript 인터페이스 설정 완료")
        
        // WebView 클라이언트 설정
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                Log.d(TAG, "URL 로드 시도: ${request?.url}")
                return false // 기본 WebView에서 URL 처리
            }
            
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.visibility = View.VISIBLE
                Log.d(TAG, "페이지 로드 시작: $url")
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                // 페이지 로딩 완료 시 프로그레스바 숨기기
                binding.progressBar.visibility = View.GONE
                Log.d(TAG, "페이지 로딩 완료: $url")
                
                // Android 인터페이스가 제대로 설정되었는지 확인
                view?.evaluateJavascript(
                    "javascript:(function() { " +
                            "console.log('Android 인터페이스 사용 가능 여부: ' + (window.Android !== undefined));" +
                            "if (window.Android === undefined) { " +
                            "  console.error('Android 인터페이스 없음!');" +
                            "} else { " +
                            "  console.log('Android 인터페이스 사용 가능 - 버전: ' + (window.Android.getAndroidVersion ? window.Android.getAndroidVersion() : 'unknown'));" +
                            "}" +
                            "})();",
                    null
                )
                
                super.onPageFinished(view, url)
            }
            
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
                Log.e(TAG, "WebView 오류 발생: ${error?.description}")
                binding.progressBar.visibility = View.GONE
                super.onReceivedError(view, request, error)
            }
        }
        
        // 디버깅 메시지를 보기 위한 WebChromeClient 설정
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d(TAG, "WebView 콘솔: ${it.message()} (${it.lineNumber()})")
                }
                return true
            }
            
            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: android.webkit.GeolocationPermissions.Callback?) {
                callback?.invoke(origin, true, false)
            }
            
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
                Log.d(TAG, "JavaScript Alert: $message")
                return super.onJsAlert(view, url, message, result)
            }
        }
        
        // 초기에 WebView 컨테이너 숨기기
        binding.webViewContainer.visibility = View.GONE
    }
    
    private fun setupListeners() {
        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        
        // 주소 입력 필드 클릭 시 주소 검색 WebView 표시
        binding.tvAddress.setOnClickListener {
            showAddressWebView()
        }
        
        // WebView 닫기 버튼
        binding.btnCloseWebView.setOnClickListener {
            hideAddressWebView()
        }
        
        // 다음 버튼 클릭 시 입력된 주소 저장 후 다음 화면으로 이동
        binding.btnContinue.setOnClickListener {
            if (validateAddress()) {
                saveAddressAndNavigate()
            }
        }
    }
    
    private fun showAddressWebView() {
        // WebView 컨테이너를 전체 화면으로 표시
        binding.webViewContainer.visibility = View.VISIBLE
        binding.progressBar.visibility = View.VISIBLE
        
        // 다음 버튼 숨기기
        binding.btnContinue.visibility = View.GONE
        
        try {
            // WebView 캐시 삭제 및 리셋
            binding.webView.clearCache(true)
            binding.webView.clearHistory()
            
            // JavaScript 인터페이스 재등록 확인
            binding.webView.removeJavascriptInterface("Android")
            binding.webView.addJavascriptInterface(WebViewInterface(), "Android")
            
            // assets 폴더의 daum_address.html 파일을 로드
            binding.webView.loadUrl("file:///android_asset/daum_address.html")
            Log.d(TAG, "daum_address.html 로드 시도")
            
            // 5초 뒤에 아무 반응이 없으면 자동으로 재로드
            binding.webView.postDelayed({
                if (binding.webViewContainer.visibility == View.VISIBLE && binding.progressBar.visibility == View.VISIBLE) {
                    Log.d(TAG, "재로드 시도")
                    binding.webView.reload()
                }
            }, 5000)
            
        } catch (e: Exception) {
            Log.e(TAG, "WebView 로드 오류: ${e.message}")
            Toast.makeText(requireContext(), "주소 검색 기능을 로드하는데 문제가 발생했습니다.", Toast.LENGTH_SHORT).show()
            hideAddressWebView()
        }
    }
    
    private fun hideAddressWebView() {
        binding.webViewContainer.visibility = View.GONE
        
        // 다음 버튼 다시 표시
        binding.btnContinue.visibility = View.VISIBLE
    }
    
    private fun validateAddress(): Boolean {
        val address = binding.tvAddress.text.toString().trim()
        if (address.isEmpty() || address == "주소 입력") {
            Toast.makeText(requireContext(), "주소를 입력해주세요", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
    
    private fun saveAddressAndNavigate() {
        val address = binding.tvAddress.text.toString().trim()
        
        // 주소 정보 저장
        SignupDataHolder.address = address
        Log.d(TAG, "주소 저장 완료: $address")
        
        // 다음 화면으로 이동 (프로필 이미지 설정 화면)
        val signupProfilePictureFragment = SignupProfilePictureFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, signupProfilePictureFragment)
            .addToBackStack(null)
            .commit()
    }
    
    // WebView와 안드로이드 간 통신을 위한 인터페이스
    inner class WebViewInterface {
        @JavascriptInterface
        fun processDATA(jsonData: String) {
            Log.d(TAG, "주소 데이터 수신: $jsonData")
            try {
                // JSON 파싱
                val data = org.json.JSONObject(jsonData)
                val address = if (data.getString("userSelectedType") == "R") {
                    data.getString("roadAddress")
                } else {
                    data.getString("jibunAddress")
                }
                
                // UI 스레드에서 주소 텍스트 업데이트
                activity?.runOnUiThread {
                    binding.tvAddress.text = address
                    hideAddressWebView()
                    Toast.makeText(requireContext(), "주소가 선택되었습니다", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "선택된 주소: $address")
                }
            } catch (e: Exception) {
                Log.e(TAG, "주소 데이터 처리 중 오류 발생: ${e.message}")
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "주소 정보를 가져오는데 실패했습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        @JavascriptInterface
        fun setAddress(address: String) {
            Log.d(TAG, "setAddress 호출됨: $address")
            if (address.isNotEmpty()) {
                // UI 스레드에서 주소 텍스트 업데이트
                activity?.runOnUiThread {
                    binding.tvAddress.text = address
                    hideAddressWebView()
                    Toast.makeText(requireContext(), "주소가 선택되었습니다", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "선택된 주소: $address")
                }
            } else {
                Log.e(TAG, "빈 주소가 전달됨")
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "유효한 주소가 선택되지 않았습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }

        @JavascriptInterface
        public fun getAndroidVersion(): Int {
            return android.os.Build.VERSION.SDK_INT
        }

        @JavascriptInterface
        public fun isAndroidInterface(): Boolean {
            return true
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}