package com.example.maite.ui.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.maite.R

class SubscriptionFragment : Fragment() {

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
        
        // 뒤로가기 버튼
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        
        // 현재 요금제 정보 설정 (예시)
        setupCurrentPlanInfo()
        
        // 요금제 버튼 클릭 이벤트 설정
        setupPlanButtons()
    }
    
    private fun setupCurrentPlanInfo() {
        // 현재는 예시 데이터로 표시
        val currentPlanName = view?.findViewById<TextView>(R.id.tv_current_plan_name)
        val currentPlanDesc = view?.findViewById<TextView>(R.id.tv_current_plan_desc)
        val currentPlanExpiry = view?.findViewById<TextView>(R.id.tv_current_plan_expiry)
        
        // 사용자의 현재 요금제 정보를 서버에서 가져와 표시
        // 예: API 호출로 현재 요금제 정보를 가져와 할당
        currentPlanName?.text = "무료 요금제" // 예시 데이터
        currentPlanDesc?.text = "기본 기능만 이용 가능"
        currentPlanExpiry?.text = "만료일: 무제한"
    }
    
    private fun setupPlanButtons() {
        // 무료 요금제 버튼
        view?.findViewById<Button>(R.id.btn_select_free)?.setOnClickListener {
            // 이미 무료 요금제 사용 중이므로 처리 없음
            Toast.makeText(context, "이미 무료 요금제를 이용 중입니다.", Toast.LENGTH_SHORT).show()
        }
        
        // 베이직 요금제 버튼
        view?.findViewById<Button>(R.id.btn_select_basic)?.setOnClickListener {
            processPurchase("베이직")
        }
        
        // 프리미엄 요금제 버튼
        view?.findViewById<Button>(R.id.btn_select_premium)?.setOnClickListener {
            processPurchase("프리미엄")
        }
    }
    
    private fun processPurchase(planName: String) {
        // 결제 처리 로직
        // 실제 앱에서는 결제 SDK를 연동하여 처리
        Toast.makeText(context, "$planName 요금제 결제가 준비 중입니다.", Toast.LENGTH_SHORT).show()
    }
}
