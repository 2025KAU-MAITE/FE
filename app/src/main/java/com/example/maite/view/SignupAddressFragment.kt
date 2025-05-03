package com.example.maite.view

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
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
        
        setupListeners()
    }
    
    private fun setupListeners() {
        // Back button click listener
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        
        // Address field click listener - open Kakao address search
        binding.tvAddress.setOnClickListener {
            openKakaoAddressSearch()
        }
        
        // Continue button click listener
        binding.btnContinue.setOnClickListener {
            if (binding.tvAddress.text.toString() == binding.tvAddress.hint.toString() || 
                binding.tvAddress.text.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "주소를 입력해주세요", Toast.LENGTH_SHORT).show()
            } else {
                // Save address and navigate to the next screen
                saveAddressAndNavigate()
            }
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun openKakaoAddressSearch() {
        // WebView를 표시할 컨테이너 보이게 설정
        binding.webViewContainer.visibility = View.VISIBLE
        
        // WebView 설정
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.addJavascriptInterface(WebViewInterface(), "Android")
        
        // WebViewClient 설정
        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }
        }
        
        // 카카오 우편번호 서비스 로드
        binding.webView.loadUrl("https://t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js")
        binding.webView.loadUrl("https://ddaaee.github.io/daum-post-code/")
    }
    
    // JavaScript 인터페이스 클래스
    inner class WebViewInterface {
        @JavascriptInterface
        fun processDATA(address: String) {
            activity?.runOnUiThread {
                // 주소 텍스트뷰에 선택한 주소 표시
                binding.tvAddress.text = address
                
                // WebView 컨테이너 숨기기
                binding.webViewContainer.visibility = View.GONE
                
                Log.d(TAG, "주소 선택 완료: $address")
            }
        }
    }
    
    private fun saveAddressAndNavigate() {
        // 주소 저장
        val address = binding.tvAddress.text.toString()
        SignupDataHolder.address = address
        
        Log.d(TAG, "주소 저장 완료: $address")
        
        // Navigate to the profile picture screen
        val signupProfilePictureFragment = SignupProfilePictureFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, signupProfilePictureFragment)
            .addToBackStack(null)
            .commit()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}