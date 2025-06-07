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
        // 현재 구독 상태 확인
        checkCurrentSubscriptionStatus()
        
        // 프리미엄 구독 버튼 클릭 리스너 설정
        binding.btnSelectPremium.setOnClickListener {
            startKakaoPayment()
        }
        
        // 기본 요금제 버튼 클릭 리스너 설정 (다운그레이드)
        binding.btnSelectFree.setOnClickListener {
            showDowngradeConfirmDialog()
        }
        
        // 뒤로가기 버튼 설정
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
    
    /**
     * 현재 구독 상태를 확인하고 UI 업데이트
     */
    private fun checkCurrentSubscriptionStatus() {
        val prefsUtil = PreferencesUtil(requireContext())
        val isSubscribed = prefsUtil.isSubscribed()
        
        Log.d(TAG, "현재 구독 상태 확인: $isSubscribed")
        
        if (isSubscribed) {
            // 이미 구독 중인 경우 UI 업데이트
            updateUIForSubscribedUser()
        } else {
            // 구독하지 않은 경우 기본 UI 유지
            updateUIForNonSubscribedUser()
        }
    }
    
    /**
     * 구독 중인 사용자를 위한 UI 업데이트
     */
    private fun updateUIForSubscribedUser() {
        // 프리미엄 버튼 - 현재 구독중 상태로 변경
        binding.btnSelectPremium.text = "현재 구독중"
        binding.btnSelectPremium.isEnabled = false
        
        // 기본 요금제 버튼 - 다운그레이드 가능 상태로 변경
        binding.btnSelectFree.text = "다운그레이드"
        binding.btnSelectFree.isEnabled = true
        
        Log.d(TAG, "구독 중인 사용자 UI로 업데이트 완료")
    }
    
    /**
     * 구독하지 않은 사용자를 위한 UI 업데이트
     */
    private fun updateUIForNonSubscribedUser() {
        // 프리미엄 버튼 - 업그레이드 가능 상태로 변경
        binding.btnSelectPremium.text = "업그레이드"
        binding.btnSelectPremium.isEnabled = true
        
        // 기본 요금제 버튼 - 현재 이용중 상태로 변경
        binding.btnSelectFree.text = "현재 이용중"
        binding.btnSelectFree.isEnabled = false
        
        Log.d(TAG, "구독하지 않은 사용자 UI로 업데이트 완료")
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
                            
                            // 현재 화면의 UI도 즉시 업데이트
                            updateUIForSubscribedUser()
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
        if (show) {
            // 로딩 중일 때 두 버튼 모두 비활성화
            binding.btnSelectPremium.isEnabled = false
            binding.btnSelectPremium.text = "처리 중..."
            binding.btnSelectFree.isEnabled = false
        } else {
            // 로딩 완료 후 현재 구독 상태에 따라 UI 복원
            checkCurrentSubscriptionStatus()
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView() 
        clearPaymentData()
        _binding = null
    }
    
    override fun onResume() {
        super.onResume()
        // 화면이 다시 표시될 때마다 구독 상태 확인
        checkCurrentSubscriptionStatus()
    }
    
    /**
     * 다운그레이드 확인 다이얼로그 표시
     */
    private fun showDowngradeConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("기본 요금제로 변경")
            .setMessage("프리미엄 구독을 취소하고 기본 요금제로 변경하시겠습니까?\n\n변경 후에는 프리미엄 기능 이용이 제한됩니다.")
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
                processDowngrade()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    
    /**
     * 다운그레이드 처리
     */
    private fun processDowngrade() {
        lifecycleScope.launch {
            try {
                showLoading(true)
                
                // 구독 상태를 로컬에서 false로 변경
                val prefsUtil = PreferencesUtil(requireContext())
                prefsUtil.saveSubscriptionStatus(false)
                
                Log.d(TAG, "구독 상태가 false로 업데이트됨 (다운그레이드)")
                
                // UI 즉시 업데이트
                updateUIForNonSubscribedUser()
                
                // 성공 메시지 표시
                showToast("기본 요금제로 변경되었습니다")
                
                // 서버에도 구독 취소 요청 (실제 API가 있다면)
                // val response = paymentRepository.cancelSubscription()
                
            } catch (e: Exception) {
                Log.e(TAG, "다운그레이드 처리 중 오류 발생", e)
                showToast("요금제 변경 중 오류가 발생했습니다")
            } finally {
                showLoading(false)
            }
        }
    }
}