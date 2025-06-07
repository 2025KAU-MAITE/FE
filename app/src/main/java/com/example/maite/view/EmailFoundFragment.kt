package com.example.maite.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.maite.R
import com.example.maite.databinding.FragmentEmailFoundBinding

class EmailFoundFragment : Fragment() {

    private var _binding: FragmentEmailFoundBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmailFoundBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set the actual ID from arguments if provided
        arguments?.getString("userId")?.let {
            binding.tvActualId.text = it
        }

        // Set up navigation to Login screen
        binding.btnLogin.setOnClickListener {
            Log.d("EmailFoundFragment", "Login button clicked - Fragment state: isAdded=$isAdded, isDetached=$isDetached, activity=${activity != null}")
            
            // Immediately disable button to prevent multiple clicks
            binding.btnLogin.isEnabled = false
            
            // Check if fragment is still valid before navigation
            if (isAdded && !isDetached && activity != null) {
                try {
                    // Double-check again right before the operation
                    if (!isAdded || isDetached || activity == null) {
                        Log.w("EmailFoundFragment", "Fragment state changed during click handling")
                        return@setOnClickListener
                    }
                    
                    // Pop back to the login activity
                    val currentActivity = activity
                    if (currentActivity is LoginActivity) {
                        Log.d("EmailFoundFragment", "Calling showLoginUI() - LoginActivity will handle backstack clearing")
                        currentActivity.showLoginUI()
                        // Note: LoginActivity.showLoginUI() will handle backstack clearing
                    }
                } catch (e: Exception) {
                    Log.e("EmailFoundFragment", "Error during login navigation", e)
                    // Re-enable button if error occurs
                    if (isAdded && !isDetached && _binding != null) {
                        binding.btnLogin.isEnabled = true
                    }
                }
            } else {
                Log.w("EmailFoundFragment", "Fragment not in valid state for navigation")
                // Re-enable button if not in valid state
                if (_binding != null) {
                    binding.btnLogin.isEnabled = true
                }
            }
        }

        // Set up navigation to Find Password screen
        binding.btnFindPassword.setOnClickListener {
            // Check if fragment is still valid before navigation
            if (isAdded && !isDetached && activity != null) {
                navigateToFindPasswordFragment()
            }
        }
    }
    
    private fun navigateToFindPasswordFragment() {
        // Double-check fragment state before navigation
        if (!isAdded || isDetached || activity == null) return
        
        try {
            val findPasswordFragment = FindPasswordFragment.newInstance()
            val currentActivity = activity
            if (currentActivity is LoginActivity) {
                currentActivity.supportFragmentManager.beginTransaction()
                    .replace(R.id.login_container, findPasswordFragment)
                    .addToBackStack(findPasswordFragment.javaClass.simpleName)
                    .commit()
            }
        } catch (e: Exception) {
            Log.e("EmailFoundFragment", "Error during find password navigation", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = EmailFoundFragment()
    }
}