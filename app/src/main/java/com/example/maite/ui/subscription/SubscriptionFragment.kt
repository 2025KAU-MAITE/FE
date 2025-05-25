package com.example.maite.ui.subscription

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.maite.PreferencesUtil
import com.example.maite.R
import com.example.maite.repository.PaymentRepository
import com.example.maite.ui.payment.KakaoPayWebViewActivity
import kotlinx.coroutines.launch

class SubscriptionFragment : Fragment() {

    private lateinit var preferencesUtil: PreferencesUtil
    private lateinit var paymentRepository: PaymentRepository
    
    // 카카오페이 결제 정보를 저장할 변수들
    private var currentTid: String? = null
    private var currentPartnerOrderId: String? = null
    private var currentPartnerUserId: String? = null
    
    // 카카오페이 WebView 액티비티 결과 처리
    private val kakaoPayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleKakaoPayResult(result.resultCode, result.data)
    }

    companion object {
        private const val TAG = "SubscriptionFragment"
        private const val PREMIUM_AMOUNT = 9900  // 프리미엄 요금제 가격 (9,900원)
        private const val PREMIUM_PLAN_NAME = "MAITE 프리미엄 플랜"
        
        fun newInstance(): SubscriptionFragment {
            return SubscriptionFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_subscription, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesUtil = PreferencesUtil(requireContext())
        paymentRepository = PaymentRepository(requireContext())
        
        // 뒤로가기 버튼
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        // 현재 요금제 정보 설정
        setupCurrentPlanInfo()
        
        // 요금제 버튼 클릭 이벤트 설정
        setupPlanButtons()
    }
    
    private fun setupCurrentPlanInfo() {
        val isPremium = preferencesUtil.getUserPremiumStatus()
        
        // 버튼 상태 업데이트
        updateButtonStates(isPremium)
    }
    
    private fun updateButtonStates(isPremium: Boolean) {
        val btnFree = view?.findViewById<Button>(R.id.btn_select_free)
        val btnPremium = view?.findViewById<Button>(R.id.btn_select_premium)
        
        // 컨텍스트가 null이 아닌 경우에만 색상을 가져옴
        val context = context ?: return
        val mainColor = context.getColor(R.color.mainColor)
        val grayColor = context.getColor(R.color.gray)
        
        if (isPremium) {
            // 프리미엄 사용자
            btnFree?.text = "다운그레이드"
            btnFree?.backgroundTintList = android.content.res.ColorStateList.valueOf(grayColor)
            btnPremium?.text = "현재 이용중"
            btnPremium?.backgroundTintList = android.content.res.ColorStateList.valueOf(mainColor)
            btnPremium?.isEnabled = false
        } else {
            // 무료 사용자
            btnFree?.text = "현재 이용중"
            btnFree?.backgroundTintList = android.content.res.ColorStateList.valueOf(mainColor)
            btnFree?.isEnabled = false
            btnPremium?.text = "업그레이드"
            btnPremium?.backgroundTintList = android.content.res.ColorStateList.valueOf(mainColor)
            btnPremium?.isEnabled = true
        }
    }
    
    private fun setupPlanButtons() {
        val isPremium = preferencesUtil.getUserPremiumStatus()
        
        // 무료 요금제 버튼
        view?.findViewById<Button>(R.id.btn_select_free)?.setOnClickListener {
            if (isPremium) {
                // 다운그레이드 처리
                showDowngradeDialog()
            } else {
                Toast.makeText(context, "이미 무료 요금제를 이용 중입니다.", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 프리미엄 요금제 버튼
        view?.findViewById<Button>(R.id.btn_select_premium)?.setOnClickListener {
            if (isPremium) {
                Toast.makeText(context, "이미 프리미엄 요금제를 이용 중입니다.", Toast.LENGTH_SHORT).show()
            } else {
                // 바로 결제 시작
                startKakaoPayment()
            }
        }
    }
    
    private fun showUpgradeDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("프리미엄 업그레이드")
            .setMessage("MAITE 프리미엄 플랜 (월 9,900원)으로 업그레이드하시겠습니까?\n\n프리미엄 플랜 혜택:\n• 무제한 AI 대화\n• 고급 기능 이용\n• 광고 제거")
            .setPositiveButton("결제하기") { _, _ ->
                startKakaoPayment()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    /**
     * 카카오페이 결제 프로세스 시작
     */
    private fun startKakaoPayment() {
        lifecycleScope.launch {
            try {
                showLoadingState(true)
                Log.d(TAG, "카카오페이 결제 준비 시작")
                
                // 1단계: 카카오페이 ready API 호출
                val readyResponse = paymentRepository.readyKakaoPayment(
                    totalAmount = PREMIUM_AMOUNT,
                    itemName = PREMIUM_PLAN_NAME,
                    quantity = 1
                )
                
                if (readyResponse.isSuccess) {
                    val result = readyResponse.result
                    // 결제 정보 저장
                    currentTid = result.tid
                    currentPartnerOrderId = result.partnerOrderId
                    currentPartnerUserId = result.partnerUserId
                    
                    Log.d(TAG, "카카오페이 ready 성공: tid=${result.tid}")
                    
                    // 2단계: WebView에서 결제 진행
                    openKakaoPayWebView(result.nextRedirectMobileUrl)
                } else {
                    showError(readyResponse.message ?: "결제 준비에 실패했습니다.")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "카카오페이 결제 준비 실패", e)
                showError("결제 준비 중 오류가 발생했습니다: ${e.message}")
            } finally {
                showLoadingState(false)
            }
        }
    }
    
    /**
     * 카카오페이 WebView 열기
     */
    private fun openKakaoPayWebView(redirectUrl: String) {
        Log.d(TAG, "카카오페이 WebView 열기: $redirectUrl")
        
        val intent = Intent(requireContext(), KakaoPayWebViewActivity::class.java).apply {
            putExtra(KakaoPayWebViewActivity.EXTRA_PAYMENT_URL, redirectUrl)
        }
        
        kakaoPayLauncher.launch(intent)
    }
    
    /**
     * 카카오페이 WebView 결과 처리
     */
    private fun handleKakaoPayResult(resultCode: Int, data: Intent?) {
        when (resultCode) {
            KakaoPayWebViewActivity.RESULT_SUCCESS -> {
                val pgToken = data?.getStringExtra(KakaoPayWebViewActivity.EXTRA_PG_TOKEN)
                if (!pgToken.isNullOrEmpty()) {
                    Log.d(TAG, "카카오페이 결제 성공 - pg_token 받음")
                    processPaymentSuccess(pgToken)
                } else {
                    showError("결제 정보를 확인할 수 없습니다.")
                }
            }
            
            KakaoPayWebViewActivity.RESULT_CANCEL -> {
                Log.d(TAG, "카카오페이 결제 취소")
                Toast.makeText(requireContext(), "결제가 취소되었습니다.", Toast.LENGTH_SHORT).show()
            }
            
            KakaoPayWebViewActivity.RESULT_FAIL -> {
                val errorMessage = data?.getStringExtra(KakaoPayWebViewActivity.EXTRA_ERROR_MESSAGE)
                Log.e(TAG, "카카오페이 결제 실패: $errorMessage")
                showError(errorMessage ?: "결제에 실패했습니다.")
            }
        }
    }
    
    /**
     * 결제 성공 처리
     */
    private fun processPaymentSuccess(pgToken: String) {
        lifecycleScope.launch {
            try {
                showLoadingState(true)
                Log.d(TAG, "카카오페이 success API 호출 시작")
                
                // 3단계: 카카오페이 success API 호출
                // 필수 정보가 모두 있는지 확인
                val tid = currentTid
                val partnerOrderId = currentPartnerOrderId
                val partnerUserId = currentPartnerUserId
                
                if (tid.isNullOrEmpty() || partnerOrderId.isNullOrEmpty() || partnerUserId.isNullOrEmpty()) {
                    showError("결제 정보가 누락되어 처리할 수 없습니다.")
                    return@launch
                }
                
                val successResponse = paymentRepository.processKakaoPaymentSuccess(
                    tid = tid,
                    partnerOrderId = partnerOrderId,
                    partnerUserId = partnerUserId,
                    pgToken = pgToken
                )
                
                if (successResponse.isSuccess) {
                    Log.d(TAG, "카카오페이 success API 성공")
                    
                    // 4단계: 사용자 정보 업데이트
                    refreshUserSubscriptionStatus()
                    
                } else {
                    showError(successResponse.message ?: "결제 완료 처리에 실패했습니다.")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "카카오페이 success 처리 실패", e)
                showError("결제 완료 처리 중 오류가 발생했습니다: ${e.message}")
            } finally {
                showLoadingState(false)
            }
        }
    }
    
    /**
     * 사용자 구독 상태 새로고침
     */
    private fun refreshUserSubscriptionStatus() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "사용자 정보 새로고침 시작")
                
                // 5단계: 사용자 정보 새로고침
                val token = preferencesUtil.getAccessToken()
                if (!token.isNullOrEmpty()) {
                    paymentRepository.refreshUserInfo(token)
                }
                
                // 로컬 상태 업데이트
                preferencesUtil.setUserPremiumStatus(true)
                setupCurrentPlanInfo()
                
                // 성공 메시지 표시
                Toast.makeText(
                    requireContext(), 
                    "프리미엄 요금제로 업그레이드되었습니다!", 
                    Toast.LENGTH_LONG
                ).show()
                
                Log.d(TAG, "프리미엄 업그레이드 완료")
                
            } catch (e: Exception) {
                Log.e(TAG, "사용자 정보 새로고침 실패", e)
                // 결제는 성공했지만 상태 업데이트 실패
                Toast.makeText(
                    requireContext(), 
                    "결제는 완료되었지만 상태 업데이트에 실패했습니다. 잠시 후 다시 확인해주세요.", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    /**
     * 로딩 상태 표시/숨김
     */
    private fun showLoadingState(isLoading: Boolean) {
        view?.findViewById<Button>(R.id.btn_select_premium)?.apply {
            isEnabled = !isLoading
            text = if (isLoading) "결제 진행 중..." else {
                if (preferencesUtil.getUserPremiumStatus()) "현재 이용중" else "업그레이드"
            }
        }
    }
    
    /**
     * 에러 메시지 표시
     */
    private fun showError(message: String) {
        Log.e(TAG, "에러: $message")
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun showDowngradeDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("구독 취소")
            .setMessage("정말로 프리미엄 구독을 취소하시겠습니까?")
            .setPositiveButton("취소하기") { _, _ ->
                // 실제로는 구독 취소 API 호출 필요
                Toast.makeText(context, "구독 취소 기능은 준비 중입니다.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("돌아가기", null)
            .show()
    }
}
