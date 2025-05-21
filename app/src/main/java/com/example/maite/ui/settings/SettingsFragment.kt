package com.example.maite.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.maite.R
import com.example.maite.UserManager
import com.example.maite.view.LoginActivity

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 뒤로가기 버튼
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
        
        // 주소 입력 필드
        view.findViewById<View>(R.id.et_address).setOnClickListener {
            showNotImplementedToast("주소 변경")
        }
        
        // 로그아웃 버튼
        view.findViewById<View>(R.id.btn_logout).setOnClickListener {
            logout()
        }
    }
    
    private fun showNotImplementedToast(feature: String) {
        Toast.makeText(requireContext(), "$feature 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show()
    }
    
    private fun logout() {
        // 사용자 데이터 제거
        UserManager.clearUserData()
        
        // 로그인 화면으로 이동
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }
}
