package com.example.maite.ui.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.maite.PreferencesUtil
import com.example.maite.R

class SubscriptionFragment : Fragment() {

    private lateinit var preferencesUtil: PreferencesUtil

    companion object {
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
        
        // 뒤로가기 버튼
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
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
        val mainColor = context?.getColor(R.color.mainColor) ?: 0
        val grayColor = context?.getColor(R.color.gray) ?: 0
        
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
                // 업그레이드 처리
                showUpgradeDialog()
            }
        }
    }
    
    private fun showUpgradeDialog() {
        // 실제로는 결제 시스템 연동 필요
        // 여기서는 시뮬레이션으로 처리
        Toast.makeText(context, "프리미엄 요금제 결제 기능은 준비 중입니다.", Toast.LENGTH_LONG).show()
        
        // 테스트용: 프리미엄으로 변경
        // preferencesUtil.setUserPremiumStatus(true)
        // setupCurrentPlanInfo()
        // Toast.makeText(context, "프리미엄 요금제로 업그레이드되었습니다!", Toast.LENGTH_SHORT).show()
    }
    
    private fun showDowngradeDialog() {
        // 실제로는 구독 취소 API 호출 필요
        Toast.makeText(context, "구독 취소 기능은 준비 중입니다.", Toast.LENGTH_LONG).show()
        
        // 테스트용: 무료로 변경
        // preferencesUtil.setUserPremiumStatus(false)
        // setupCurrentPlanInfo()
        // Toast.makeText(context, "무료 요금제로 변경되었습니다.", Toast.LENGTH_SHORT).show()
    }
}
