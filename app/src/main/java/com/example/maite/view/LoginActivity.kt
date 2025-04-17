package com.example.maite.view

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.maite.R
import com.example.maite.viewmodel.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var viewModel: LoginViewModel
    
    // Views would be initialized using View Binding in a production app
    // This is simplified for demonstration
    private lateinit var etEmail: android.widget.EditText
    private lateinit var etPassword: android.widget.EditText
    private lateinit var btnLogin: android.widget.Button
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var tvForgotPassword: android.widget.TextView
    private lateinit var tvSignUp: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        
        // Initialize views
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        progressBar = findViewById(R.id.progressBar)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvSignUp = findViewById(R.id.tvSignUp)
        
        // Set up click listeners
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            viewModel.login(email, password)
        }
        
        tvForgotPassword.setOnClickListener {
            // Navigate to forgot password screen
            Toast.makeText(this, "비밀번호 재설정 화면으로 이동합니다", Toast.LENGTH_SHORT).show()
        }
        
        tvSignUp.setOnClickListener {
            // Navigate to sign up screen
            Toast.makeText(this, "회원가입 화면으로 이동합니다", Toast.LENGTH_SHORT).show()
        }
        
        // Observe ViewModel's LiveData
        viewModel.loginResult.observe(this, Observer { result ->
            result?.let {
                if (it.success) {
                    // Navigate to main screen or next screen
                    Toast.makeText(this, "로그인 성공: ${it.user?.name}님 환영합니다", Toast.LENGTH_LONG).show()
                    // Here you would typically navigate to the main activity
                    // For example: startActivity(Intent(this, MainActivity::class.java))
                } else {
                    // Show error message
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                }
            }
        })
        
        viewModel.isLoading.observe(this, Observer { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnLogin.isEnabled = !isLoading
        })
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Reset ViewModel state to prevent showing old results on activity recreation
        viewModel.resetLoginState()
    }
}