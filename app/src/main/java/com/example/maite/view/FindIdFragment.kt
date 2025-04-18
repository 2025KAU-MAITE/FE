package com.example.maite.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.example.maite.R

class FindIdFragment : Fragment() {

    private lateinit var etEmail: EditText
    private lateinit var btnFindId: Button
    private lateinit var btnBack: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_id, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        etEmail = view.findViewById(R.id.etEmail)
        btnFindId = view.findViewById(R.id.btnFindId)
        btnBack = view.findViewById(R.id.btnBack)

        // Set click listener for back button
        btnBack.setOnClickListener {
            // Go back to the previous screen
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Set click listener for find ID button
        btnFindId.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                // Implement ID recovery logic here
                findIdWithEmail(email)
            } else {
                // Show error message if email is empty
                etEmail.error = "이메일을 입력해주세요"
            }
        }
    }

    private fun findIdWithEmail(email: String) {
        // This is where you would implement the logic to find the user's ID
        // For example, making a network request to your backend
        
        // For now, we'll just handle the UI flow
        // In a real app, this would connect to your backend service
    }

    companion object {
        @JvmStatic
        fun newInstance() = FindIdFragment()
    }
}