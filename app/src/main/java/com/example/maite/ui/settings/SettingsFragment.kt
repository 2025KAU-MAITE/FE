package com.example.maite.ui.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.maite.ApiClient
import com.example.maite.PreferencesUtil
import com.example.maite.R
import com.example.maite.UserManager
import com.example.maite.model.AuthApi
import com.example.maite.view.LoginActivity
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    
    private lateinit var preferencesUtil: PreferencesUtil

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // PreferencesUtil 초기화
        preferencesUtil = PreferencesUtil(requireContext())
        
        // 현재 요금제 정보 표시
        setupCurrentPlanInfo()
        
        // 뒤로가기 버튼
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        
        // 로그아웃 버튼
        view.findViewById<View>(R.id.btn_logout).setOnClickListener {
            logout()
        }
        
        // 요금제 관리 버튼
        view.findViewById<View>(R.id.btn_manage_plan).setOnClickListener {
            openPlanManagement()
        }
    }
    
    private fun setupCurrentPlanInfo() {
        // SharedPreferences에서 현재 요금제 정보 가져오기
        val currentPlan = preferencesUtil.getCurrentPlan()
        
        // TextView에 요금제 정보 설정
        view?.findViewById<TextView>(R.id.tv_current_plan)?.text = "현재 요금제: $currentPlan"
        
        Log.d("SettingsFragment", "현재 요금제 정보 설정: $currentPlan")
    }
    
    private fun openPlanManagement() {
        try {
            // SubscriptionFragment로 이동
            val subscriptionFragment = com.example.maite.ui.subscription.SubscriptionFragment.newInstance()
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.main_frm, subscriptionFragment)
                .addToBackStack(null)
                .commit()
        } catch (e: Exception) {
            Log.e("SettingsFragment", "요금제 관리 화면 이동 중 오류: ${e.message}", e)
            Toast.makeText(requireContext(), "요금제 관리 화면을 표시할 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showNotImplementedToast(feature: String) {
        Toast.makeText(requireContext(), "$feature 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
    }
    
    private fun logout() {
        // 로그아웃 API 호출
        lifecycleScope.launch {
            try {
                val authApi = ApiClient.getClient(requireContext()).create(AuthApi::class.java)
                val response = authApi.logout()
                
                if (response.isSuccess) {
                    // 사용자 데이터 제거
                    UserManager.clearUserData()
                    
                    // 로그인 화면으로 이동
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity?.finish()
                } else {
                    Toast.makeText(requireContext(), "로그아웃 실패: ${response.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SettingsFragment", "로그아웃 오류: ${e.message}")
                // 네트워크 오류가 발생해도 로컬에서는 로그아웃 처리
                UserManager.clearUserData()
                
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                activity?.finish()
            }
        }
    }
}
