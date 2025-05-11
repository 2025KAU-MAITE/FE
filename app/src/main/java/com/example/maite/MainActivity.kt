package com.example.maite

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
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

        window.navigationBarColor = resources.getColor(android.R.color.black, theme)
        // 기본 선택 항목 설정
        binding.bottomNavigation.selectedItemId = R.id.navigation_home
    }

    /**
     * List 탭으로 이동하는 메서드
     * HomeFragment 등에서 호출하여 회의방 목록 화면으로 전환할 때 사용
     */
    fun navigateToListTab() {
        binding.bottomNavigation.selectedItemId = R.id.navigation_list
    }
}