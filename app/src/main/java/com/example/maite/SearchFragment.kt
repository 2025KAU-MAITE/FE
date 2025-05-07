package com.example.maite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.maite.adapter.SearchAdapter
import com.example.maite.model.AddFriendRequest
import com.example.maite.repository.UserRepository
import com.example.maite.viewmodel.SearchViewModel
import com.example.maite.viewmodel.SearchViewModelFactory
import kotlinx.coroutines.launch


class SearchFragment : Fragment() {
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var searchAdapter: SearchAdapter
    private lateinit var etSearch: EditText
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var btnSend: AppCompatButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        etSearch = view.findViewById(R.id.etSearch)
        rvSearchResults = view.findViewById(R.id.rvSearchResults)
        btnSend = view.findViewById(R.id.btnSend)
        
        // Update button text to match functionality
        btnSend.text = "친구 추가하기"

        setupViewModel()
        setupRecyclerView()
        setupSearchListener()
        observeViewModel()
        setupSendButton()
    }

    private fun setupViewModel() {
        // Use context instead of UserApiService
        val userRepository = UserRepository(requireContext())
        val factory = SearchViewModelFactory(userRepository)
        
        searchViewModel = ViewModelProvider(this, factory)[SearchViewModel::class.java]
    }

    private fun setupRecyclerView() {
        searchAdapter = SearchAdapter(mutableListOf())
        rvSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
        }
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener { text ->
            // Debounce could be added here for better UX
            val query = text.toString().trim()
            searchViewModel.searchUsers(query)
        }
    }

    private fun observeViewModel() {
        searchViewModel.searchResults.observe(viewLifecycleOwner) { users ->
            searchAdapter.updateUsers(users)
            updateButtonState()
        }

        lifecycleScope.launch {
            searchViewModel.isLoading.collect { isLoading ->
                // Toggle loading indicator visibility
                btnSend.isEnabled = !isLoading
            }
        }

        searchViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                searchViewModel.clearError()
            }
        }

        searchViewModel.friendRequestSent.observe(viewLifecycleOwner) { sent ->
            if (sent) {
                Toast.makeText(context, "친구 요청이 성공적으로 전송되었습니다", Toast.LENGTH_SHORT).show()
                // Clear selections and refresh search
                etSearch.text.clear()
                searchViewModel.resetFriendRequestSent()
                searchViewModel.searchUsers("")
            }
        }
    }

    private fun setupSendButton() {
        btnSend.setOnClickListener {
            val selectedUser = searchAdapter.getSelectedUser()
            if (selectedUser != null) {
                // Create a friend request with only the selected user's ID
                val request = AddFriendRequest(userId = selectedUser.id.toLong())
                searchViewModel.addFriend(request)
            } else {
                Toast.makeText(context, "친구 추가할 사용자를 선택해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateButtonState() {
        // Enable button only if a user is selected
        btnSend.isEnabled = searchAdapter.getSelectedUser() != null
    }

    companion object {
        @JvmStatic
        fun newInstance() = SearchFragment()
    }
}