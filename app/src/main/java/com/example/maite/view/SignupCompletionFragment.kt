package com.example.maite.view

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
        // Navigate to the main activity or home fragment
        // This will depend on your app's navigation flow
        // Option 1: Navigate to home fragment if we're already in the main activity
        // findNavController().navigate(R.id.action_signupCompletionFragment_to_homeFragment)
        
        // Option 2: Start MainActivity and finish current activity
        activity?.let {
            // Clear backstack so user can't go back to signup flow
            findNavController().popBackStack(R.id.signupFragment, false)
            findNavController().navigate(R.id.homeFragment)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}