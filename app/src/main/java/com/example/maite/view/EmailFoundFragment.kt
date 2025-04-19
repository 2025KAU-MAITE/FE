package com.example.maite.view

import android.os.Bundle
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
            // Pop back to the login activity
            requireActivity().supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }

        // Set up navigation to Find Password screen
        binding.btnFindPassword.setOnClickListener {
            navigateToFindPasswordFragment()
        }
    }
    
    private fun navigateToFindPasswordFragment() {
        val findPasswordFragment = FindPasswordFragment.newInstance()
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, findPasswordFragment)
            .addToBackStack(null)
            .commit()
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