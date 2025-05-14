package com.example.maite

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.maite.databinding.FragmentChatListBinding
import com.example.maite.model.ChatListRepository
import com.example.maite.view.ChatListAdapter
import com.example.maite.view.ChatRoomFragment
import com.example.maite.viewmodel.ChatListViewModel
import com.example.maite.viewmodel.ChatListViewModelFactory

class ChatListFragment : Fragment() {
    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChatListViewModel
    private lateinit var chatAdapter: ChatListAdapter

    companion object {
        const val TAG = "ChatListFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel 초기화
        val repository = ChatListRepository()
        val factory = ChatListViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ChatListViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // 어댑터 초기화
        chatAdapter = ChatListAdapter { chatItem ->
            val chatRoomFragment = ChatRoomFragment.newInstance(chatItem.id)
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, chatRoomFragment, ChatRoomFragment.TAG)
                .addToBackStack(null)
                .commit()
        }

        // RecyclerView 설정
        binding.rvChatList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
        }

        // 뒤로 가기 버튼
        binding.ivBackArrow.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .remove(this)
                .commit()
        }

        // 검색 기능
        binding.searchEditText.doAfterTextChanged { text ->
            viewModel.searchChats(text.toString())
        }

        // 개인/단체 토글 버튼
        binding.personalToggle.setOnClickListener {
            viewModel.loadChatList(true)
        }

        binding.groupToggle.setOnClickListener {
            viewModel.loadChatList(false)
        }
    }

    private fun observeViewModel() {
        // 채팅 목록 관찰
        viewModel.chatItems.observe(viewLifecycleOwner) { chatItems ->
            chatAdapter.submitList(chatItems)
        }

        // 로딩 상태 관찰
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // 로딩 표시 처리 (필요한 경우 ProgressBar 추가)
        }

        // 에러 메시지 관찰
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // 탭 상태 관찰
        viewModel.isPersonalTab.observe(viewLifecycleOwner) { isPersonal ->
            updateToggleUI(isPersonal)
        }
    }

    // 토글 버튼 UI 업데이트
    private fun updateToggleUI(isPersonal: Boolean) {
        if (isPersonal) {
            binding.personalToggle.setCardBackgroundColor(Color.parseColor("#A4CAFF"))
            binding.tvPersonal.setTextColor(Color.WHITE)
            binding.groupToggle.setCardBackgroundColor(Color.WHITE)
            binding.tvGroup.setTextColor(Color.GRAY)
        } else {
            binding.groupToggle.setCardBackgroundColor(Color.parseColor("#A4CAFF"))
            binding.tvGroup.setTextColor(Color.WHITE)
            binding.personalToggle.setCardBackgroundColor(Color.WHITE)
            binding.tvPersonal.setTextColor(Color.GRAY)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}