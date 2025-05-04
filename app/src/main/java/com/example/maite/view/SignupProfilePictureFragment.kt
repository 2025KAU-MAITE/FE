package com.example.maite.view

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.maite.R
import com.example.maite.databinding.FragmentSignupProfilePictureBinding
import com.example.maite.model.SignupDataHolder

class SignupProfilePictureFragment : Fragment() {

    private val TAG = "SignupProfilePictureFragment"
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
            requireActivity().supportFragmentManager.popBackStack()
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
            // 기본 이미지 설정 (프로필 기본 사람 모양 사진)
            saveDefaultProfilePicture()
            navigateToNextScreen()
        }
        
        // Continue button click listener (previously Add button)
        binding.btnContinue.setOnClickListener {
            // 선택한 이미지가 있으면 저장하고 다음 화면으로 이동
            if (selectedImageUri != null) {
                saveSelectedProfilePicture()
                navigateToNextScreen()
            } else {
                Toast.makeText(requireContext(), "프로필 사진을 선택해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }
    
    /**
     * 건너뛰기 버튼 클릭 시 기본 프로필 이미지를 저장
     */
    private fun saveDefaultProfilePicture() {
        // 기본 이미지 URL 또는 리소스 ID를 SignupDataHolder에 저장
        SignupDataHolder.profileImageUrl = "default_profile_image"
        Log.d(TAG, "기본 프로필 이미지 저장 완료")
        Toast.makeText(requireContext(), "기본 프로필 이미지가 설정되었습니다", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 사용자가 선택한 프로필 이미지를 저장
     */
    private fun saveSelectedProfilePicture() {
        // 선택한 이미지의 URI를 SignupDataHolder에 저장
        selectedImageUri?.let { uri ->
            SignupDataHolder.profileImageUrl = uri.toString()
            Log.d(TAG, "선택한 프로필 이미지 저장 완료: $uri")
            Toast.makeText(requireContext(), "프로필 이미지가 저장되었습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToNextScreen() {
        // Navigate to the signup completion screen
        val signupCompletionFragment = SignupCompletionFragment()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, signupCompletionFragment)
            .addToBackStack(null)
            .commit()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}