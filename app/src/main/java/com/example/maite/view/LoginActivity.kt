package com.example.maite.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.example.maite.MainActivity
import com.example.maite.R
import com.example.maite.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // View binding setup
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Direct navigation to MainActivity for UI testing
        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        
        // Set up navigation to Find ID fragment
        binding.tvFindId.setOnClickListener {
            navigateToFindIdFragment()
        }
        
        // Set up navigation to Find Password fragment
        binding.tvFindPassword.setOnClickListener {
            navigateToFindPasswordFragment()
        }
        
        // Set up navigation to Signup fragment
        binding.tvSignUp.setOnClickListener {
            navigateToSignupFragment()
        }
    }
    
    private fun navigateToFindIdFragment() {
        val findIdFragment = FindIdFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, findIdFragment)
            .addToBackStack(null)
            .commit()
    }
    
    private fun navigateToFindPasswordFragment() {
        val findPasswordFragment = FindPasswordFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, findPasswordFragment)
            .addToBackStack(null)
            .commit()
    }
    
    private fun navigateToSignupFragment() {
        val signupFragment = SignupFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, signupFragment)
            .addToBackStack(null)
            .commit()
    }
}