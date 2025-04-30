package com.example.maite.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.maite.R
import com.example.maite.databinding.FragmentSignupCompletionBinding

class SignupCompletionFragment : Fragment() {

    private var _binding: FragmentSignupCompletionBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupCompletionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupListeners()
    }
    
    private fun setupListeners() {
        // Start button click listener
        binding.btnStart.setOnClickListener {
            // Navigate to the main screen or home fragment
            navigateToMainScreen()
        }
    }
    
    private fun navigateToMainScreen() {
        // Navigate to MainActivity and clear back stack
        val intent = Intent(requireActivity(), com.example.maite.MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}