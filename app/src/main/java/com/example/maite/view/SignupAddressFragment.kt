package com.example.maite.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.maite.R
import com.example.maite.databinding.FragmentSignupAddressBinding

class SignupAddressFragment : Fragment() {

    private var _binding: FragmentSignupAddressBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupAddressBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupListeners()
    }
    
    private fun setupListeners() {
        // Back button click listener
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        
        // Address field click listener - directly navigate to next screen 
        binding.tvAddress.setOnClickListener {
            // Skip Kakao address search and just go to next screen
            navigateToNextScreen()
        }
        
        // Continue button click listener
        binding.btnContinue.setOnClickListener {
            if (binding.tvAddress.text.toString() == binding.tvAddress.hint.toString() || 
                binding.tvAddress.text.isNullOrEmpty()) {
                // No longer showing a toast, just proceed
                navigateToNextScreen()
            } else {
                // Save address and navigate to the next screen
                navigateToNextScreen()
            }
        }
    }
    
    private fun openKakaoAddressSearch() {
        // TODO: Integrate with Kakao address search API
        // This will be implemented with the Kakao Local API
        // For now, we'll just show a toast message
        Toast.makeText(requireContext(), "카카오 주소 검색을 실행합니다", Toast.LENGTH_SHORT).show()
        
        // Example of what would happen after selecting an address:
        // binding.tvAddress.text = "서울특별시 강남구 테헤란로 123"
    }
    
    private fun navigateToNextScreen() {
        // Navigate to the profile picture screen
        val signupProfilePictureFragment = SignupProfilePictureFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, signupProfilePictureFragment)
            .addToBackStack(null)
            .commit()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}