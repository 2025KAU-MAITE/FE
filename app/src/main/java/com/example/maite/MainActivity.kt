package com.example.maite

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.maite.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 뷰 바인딩 초기화
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 메인 컨텐츠 영역의 인셋 처리 (ConstraintLayout에 main ID가 없는 경우 rootView 사용)
        val rootView = binding.root
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0) // 하단 패딩 제거
            insets
        }

        // 네비게이션 바의 인셋 처리
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom) // 하단 인셋만 적용
            insets
        }

        // BottomNavigationView 설정
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // 홈 프래그먼트로 이동
                    supportFragmentManager.beginTransaction().replace(R.id.main_frm, HomeFragment()).commit()
                    true
                }
                R.id.navigation_list -> {
                    // 리스트 프래그먼트로 이동
                    supportFragmentManager.beginTransaction().replace(R.id.main_frm, ListFragment()).commit()
                    true
                }
                R.id.navigation_search -> {
                    // 검색 프래그먼트로 이동
                    supportFragmentManager.beginTransaction().replace(R.id.main_frm, SearchFragment()).commit()
                    true
                }
                R.id.navigation_profile -> {
                    // 프로필 프래그먼트로 이동
                    supportFragmentManager.beginTransaction().replace(R.id.main_frm, ProfileFragment()).commit()
                    true
                }
                else -> false
            }
        }

        // 기본 선택 항목 설정
        binding.bottomNavigation.selectedItemId = R.id.navigation_home
    }
}