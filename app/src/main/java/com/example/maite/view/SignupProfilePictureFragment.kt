package com.example.maite.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.maite.R
import com.example.maite.databinding.FragmentSignupProfilePictureBinding

class SignupProfilePictureFragment : Fragment() {

    private var _binding: FragmentSignupProfilePictureBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null
    
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.ivProfilePicture.setImageURI(uri)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupProfilePictureBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupListeners()
    }
    
    private fun setupListeners() {
        // Back button click listener
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Profile picture click listener
        binding.ivProfilePicture.setOnClickListener {
            openImagePicker()
        }
        
        // Add picture text click listener
        binding.tvAddPicture.setOnClickListener {
            openImagePicker()
        }
        
        // Skip button click listener
        binding.btnSkip.setOnClickListener {
            navigateToNextScreen()
        }
        
        // Continue button click listener (previously Add button)
        binding.btnContinue.setOnClickListener {
            // Save profile picture if selected and navigate to the next screen
            if (selectedImageUri != null) {
                saveProfilePicture()
            }
            navigateToNextScreen()
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }
    
    private fun saveProfilePicture() {
        // TODO: Save the profile picture to local storage or upload to server
        Toast.makeText(requireContext(), "프로필 사진이 저장되었습니다", Toast.LENGTH_SHORT).show()
    }
    
    private fun navigateToNextScreen() {
        // TODO: Navigate to the next screen in the signup flow
        // findNavController().navigate(R.id.action_signupProfilePictureFragment_to_nextFragment)
        Toast.makeText(requireContext(), "회원가입이 완료되었습니다", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}