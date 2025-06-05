package com.example.maite.ui.subscription

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.maite.PreferencesUtil
import com.example.maite.databinding.FragmentSubscriptionBinding
import com.example.maite.repository.PaymentRepository
import com.example.maite.ui.payment.KakaoPayWebViewActivity
import kotlinx.coroutines.launch

class SubscriptionFragment : Fragment() {
    
    companion object {
        fun newInstance(): SubscriptionFragment {
            return SubscriptionFragment()
        }
    }
    
    private var _binding: FragmentSubscriptionBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var paymentRepository: PaymentRepository
    
    // 현재 결제 정보를 저장할 변수들
    private var currentTid: String? = null
    private var currentPartnerOrderId: String? = null
    private var currentPartnerUserId: String? = null
    
    private val TAG = "SubscriptionFragment"
    
    // 카카오페이 WebView 결과 처리
    private val kakaoPayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleKakaoPayResult(result.resultCode, result.data)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // PaymentRepository 초기화
        paymentRepository = PaymentRepository(requireContext())
        
        setupUI()
    }
    
    private fun setupUI() {
        // 프리미엄 구독 버튼 클릭 리스너 설정
        binding.btnSelectPremium.setOnClickListener {
            startKakaoPayment()
        }
        
        // 뒤로가기 버튼 설정
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    private fun startKakaoPayment() {
        Log.d(TAG, "카카오페이 결제 시작")
        
        lifecycleScope.launch {
            try {
                // 로딩 상태 표시
                showLoading(true)
                
                // 카카오페이 결제 준비 API 호출
                val response = paymentRepository.readyKakaoPayment(
                    totalAmount = 5000, // 구독료 5000원
                    itemName = "MAITE 앱 구독"
                )
                
                if (response.isSuccess) {
                    // 결제 정보 저장 (인스턴스 변수)
                    currentTid = response.result.tid
                    currentPartnerOrderId = response.result.partnerOrderId
                    currentPartnerUserId = response.result.partnerUserId
                    
                    // 결제 정보 백업 저장 (SharedPreferences) - null 체크 후 저장
                    val tid = response.result.tid
                    val partnerOrderId = response.result.partnerOrderId
                    val partnerUserId = response.result.partnerUserId
                    
                    if (partnerOrderId != null && partnerUserId != null) {
                        savePaymentDataToPrefs(tid, partnerOrderId, partnerUserId)
                    } else {
                        Log.e(TAG, "partnerOrderId 또는 partnerUserId가 null입니다")
                    }
                    
                    Log.d(TAG, "결제 준비 성공 - tid: $currentTid")
                    Log.d(TAG, "partnerOrderId: $currentPartnerOrderId")
                    Log.d(TAG, "partnerUserId: $currentPartnerUserId")
                    Log.d(TAG, "결제 URL: ${response.result.nextRedirectMobileUrl}")
                    
                    // WebView 액티비티 시작
                    val intent = Intent(requireContext(), KakaoPayWebViewActivity::class.java).apply {
                        putExtra(KakaoPayWebViewActivity.EXTRA_PAYMENT_URL, response.result.nextRedirectMobileUrl)
                    }
                    kakaoPayLauncher.launch(intent)
                } else {
                    Log.e(TAG, "결제 준비 실패: ${response.message}")
                    showToast("결제 준비에 실패했습니다: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "결제 준비 중 오류 발생", e)
                showToast("결제 준비 중 오류가 발생했습니다")
            } finally {
                showLoading(false)
            }
        }
    }
    
    private fun handleKakaoPayResult(resultCode: Int, data: Intent?) {
        Log.d(TAG, "카카오페이 결제 결과 - resultCode: $resultCode")
        Log.d(TAG, "WebView 결과 받은 후 - currentTid: $currentTid")
        Log.d(TAG, "WebView 결과 받은 후 - currentPartnerOrderId: $currentPartnerOrderId")
        Log.d(TAG, "WebView 결과 받은 후 - currentPartnerUserId: $currentPartnerUserId")
        
        when (resultCode) {
            KakaoPayWebViewActivity.RESULT_SUCCESS -> {
                val pgToken = data?.getStringExtra(KakaoPayWebViewActivity.EXTRA_PG_TOKEN)
                if (pgToken != null) {
                    Log.d(TAG, "결제 성공 - pg_token 받음")
                    processPaymentSuccess(pgToken)
                } else {
                    Log.e(TAG, "pg_token이 null입니다")
                    showToast("결제 정보를 확인할 수 없습니다")
                    clearPaymentData()
                }
            }
            KakaoPayWebViewActivity.RESULT_CANCEL -> {
                Log.d(TAG, "결제 취소")
                showToast("결제가 취소되었습니다")
                clearPaymentData()
            }
            KakaoPayWebViewActivity.RESULT_FAIL -> {
                val errorMessage = data?.getStringExtra(KakaoPayWebViewActivity.EXTRA_ERROR_MESSAGE)
                Log.e(TAG, "결제 실패: $errorMessage")
                showToast("결제에 실패했습니다: $errorMessage")
                clearPaymentData()
            }
            else -> {
                Log.w(TAG, "알 수 없는 결제 결과: $resultCode")
                showToast("결제 중 알 수 없는 오류가 발생했습니다")
                clearPaymentData()
            }
        }
    }
    
    private fun processPaymentSuccess(pgToken: String) {
        Log.d(TAG, "결제 성공 처리 시작")
        Log.d(TAG, "pgToken: ${pgToken.take(10)}...")
        Log.d(TAG, "currentTid: $currentTid")
        Log.d(TAG, "currentPartnerOrderId: $currentPartnerOrderId")
        Log.d(TAG, "currentPartnerUserId: $currentPartnerUserId")
        
        // 인스턴스 변수가 null인 경우 SharedPreferences에서 복원
        if (currentTid == null || currentPartnerOrderId == null || currentPartnerUserId == null) {
            Log.w(TAG, "인스턴스 변수가 null, SharedPreferences에서 복원 시도")
            restorePaymentDataFromPrefs()
            Log.d(TAG, "복원 후 - currentTid: $currentTid")
            Log.d(TAG, "복원 후 - currentPartnerOrderId: $currentPartnerOrderId")
            Log.d(TAG, "복원 후 - currentPartnerUserId: $currentPartnerUserId")
        }
        
        // 필요한 정보가 모두 있는지 확인
        if (currentTid == null || currentPartnerOrderId == null || currentPartnerUserId == null) {
            Log.e(TAG, "결제 정보가 누락되었습니다")
            Log.e(TAG, "currentTid: $currentTid")
            Log.e(TAG, "currentPartnerOrderId: $currentPartnerOrderId") 
            Log.e(TAG, "currentPartnerUserId: $currentPartnerUserId")
            showToast("결제 정보가 누락되어 처리할 수 없습니다")
            clearPaymentData()
            return
        }
        
        lifecycleScope.launch {
            try {
                showLoading(true)
                
                // 카카오페이 결제 성공 처리 API 호출
                val response = paymentRepository.processKakaoPaymentSuccess(
                    tid = currentTid!!,
                    partnerOrderId = currentPartnerOrderId!!,
                    partnerUserId = currentPartnerUserId!!,
                    pgToken = pgToken
                )
                
                if (response.isSuccess) {
                    Log.d(TAG, "결제 완료: ${response.message}")
                    showSubscriptionSuccessDialog()
                    
                    // 사용자 정보 새로고침 (구독 상태 업데이트)
                    refreshUserSubscriptionStatus()
                } else {
                    Log.e(TAG, "결제 처리 실패: ${response.message}")
                    showToast("결제 처리에 실패했습니다: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "결제 처리 중 오류 발생", e)
                showToast("결제 처리 중 오류가 발생했습니다")
            } finally {
                showLoading(false)
                clearPaymentData()
            }
        }
    }
    
    private fun showSubscriptionSuccessDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("구독 완료")
            .setMessage("MAITE 구독이 완료되었습니다!\n이제 모든 기능을 자유롭게 이용하실 수 있습니다.")
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
                // /auth/me API 호출 후 설정 화면으로 돌아가기
                refreshUserInfoAndNavigateToSettings()
            }
            .show()
    }
    
    /**
     * 사용자 정보 갱신 후 설정 화면으로 이동
     */
    private fun refreshUserInfoAndNavigateToSettings() {
        lifecycleScope.launch {
            try {
                showLoading(true)
                
                // /auth/me API 호출하여 사용자 정보 갱신
                val prefsUtil = PreferencesUtil(requireContext())
                val token = prefsUtil.getAccessToken() ?: ""
                
                if (token.isNotEmpty()) {
                    val response = paymentRepository.refreshUserInfo(token)
                    
                    if (response.isSuccess) {
                        Log.d(TAG, "구독 완료 후 사용자 정보 갱신 성공")
                        
                        // 구독 상태를 로컬에 업데이트
                        if (response.result.subscribed) {
                            prefsUtil.saveSubscriptionStatus(true)
                            Log.d(TAG, "구독 상태가 true로 업데이트됨")
                        }
                    } else {
                        Log.w(TAG, "사용자 정보 갱신 실패하지만 계속 진행: ${response.message}")
                    }
                } else {
                    Log.w(TAG, "토큰이 없어서 정보 갱신을 건너뜀")
                }
                
                // 설정 화면으로 돌아가기
                navigateToSettings()
                
            } catch (e: Exception) {
                Log.e(TAG, "사용자 정보 갱신 중 오류 발생", e)
                // 오류가 발생해도 설정 화면으로 돌아가기
                navigateToSettings()
            } finally {
                showLoading(false)
            }
        }
    }
    
    /**
     * 설정 화면으로 돌아가기
     */
    private fun navigateToSettings() {
        try {
            // SubscriptionFragment에서 설정 화면으로 돌아가기
            // Fragment를 닫고 이전 화면(설정)으로 돌아감
            parentFragmentManager.popBackStack()
            Log.d(TAG, "설정 화면으로 돌아가기 완료")
        } catch (e: Exception) {
            Log.e(TAG, "설정 화면으로 돌아가기 중 오류 발생", e)
        }
    }
    
    private fun refreshUserSubscriptionStatus() {
        lifecycleScope.launch {
            try {
                // 토큰 가져오기
                val prefsUtil = PreferencesUtil(requireContext())
                val token = prefsUtil.getAccessToken() ?: ""
                if (token.isNotEmpty()) {
                    val userInfo = paymentRepository.refreshUserInfo(token)
                    if (userInfo.isSuccess) {
                        Log.d(TAG, "사용자 정보 새로고침 완료")
                        // 필요시 UI 업데이트
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "사용자 정보 새로고침 실패", e)
            }
        }
    }
    
    private fun clearPaymentData() {
        Log.d(TAG, "결제 데이터 정리")
        currentTid = null
        currentPartnerOrderId = null
        currentPartnerUserId = null
        clearPaymentDataFromPrefs()
    }
    
    /**
     * 결제 정보를 SharedPreferences에 임시 저장
     */
    private fun savePaymentDataToPrefs(tid: String, partnerOrderId: String, partnerUserId: String) {
        try {
            val prefsUtil = PreferencesUtil(requireContext())
            prefsUtil.setString("temp_payment_tid", tid)
            prefsUtil.setString("temp_payment_partner_order_id", partnerOrderId)
            prefsUtil.setString("temp_payment_partner_user_id", partnerUserId)
            Log.d(TAG, "결제 정보 SharedPreferences에 백업 저장 완료")
        } catch (e: Exception) {
            Log.e(TAG, "결제 정보 백업 저장 실패", e)
        }
    }
    
    /**
     * SharedPreferences에서 결제 정보 복원
     */
    private fun restorePaymentDataFromPrefs() {
        try {
            val prefsUtil = PreferencesUtil(requireContext())
            currentTid = prefsUtil.getString("temp_payment_tid")
            currentPartnerOrderId = prefsUtil.getString("temp_payment_partner_order_id")
            currentPartnerUserId = prefsUtil.getString("temp_payment_partner_user_id")
            Log.d(TAG, "SharedPreferences에서 결제 정보 복원 완료")
        } catch (e: Exception) {
            Log.e(TAG, "결제 정보 복원 실패", e)
        }
    }
    
    /**
     * SharedPreferences에서 임시 결제 정보 삭제
     */
    private fun clearPaymentDataFromPrefs() {
        try {
            val prefsUtil = PreferencesUtil(requireContext())
            prefsUtil.removeString("temp_payment_tid")
            prefsUtil.removeString("temp_payment_partner_order_id")
            prefsUtil.removeString("temp_payment_partner_user_id")
            Log.d(TAG, "SharedPreferences에서 임시 결제 정보 삭제 완료")
        } catch (e: Exception) {
            Log.e(TAG, "임시 결제 정보 삭제 실패", e)
        }
    }
    
    private fun showLoading(show: Boolean) {
        // 버튼 상태만 변경 (프로그래스바가 레이아웃에 없으므로)
        binding.btnSelectPremium.isEnabled = !show
        binding.btnSelectPremium.text = if (show) "처리 중..." else "업그레이드"
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView() 
        clearPaymentData()
        _binding = null
    }
}