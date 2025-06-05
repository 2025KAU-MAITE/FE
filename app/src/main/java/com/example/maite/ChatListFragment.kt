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
import com.example.maite.model.ChatListItem
import com.example.maite.model.ChatListRepository
import com.example.maite.view.ChatListAdapter
import com.example.maite.view.ChatRoomFragment
import com.example.maite.viewmodel.ChatListViewModel
import com.example.maite.viewmodel.ChatListViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.AlertDialog
import com.example.maite.model.CreateChatRoomRequest

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
        val repository = ChatListRepository(requireContext())
        val factory = ChatListViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ChatListViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // 어댑터 초기화
        chatAdapter = ChatListAdapter { chatItem ->
            // chatItem이 사용자인지 채팅방인지 구분하여 처리
            if (chatItem.isUser) {
                // 사용자를 클릭했을 때 - 채팅방 존재 여부 확인 후 처리
                handleUserItemClick(chatItem)
            } else {
                // 채팅방을 클릭했을 때 - 채팅방으로 이동
                navigateToChatRoom(chatItem)
            }
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

        // 검색 기능 설정
        binding.searchEditText.hint = "채팅방 또는 사용자 검색..."

        binding.searchEditText.doAfterTextChanged { text ->
            val query = text.toString().trim()

            if (query.isEmpty()) {
                // 검색어가 비어있으면 채팅 목록 표시
                viewModel.loadChatList(viewModel.isPersonalTab.value ?: true)
            } else {
                // 검색어가 입력되면 사용자 검색 API 사용
                viewModel.searchUsers(query)
            }
        }

        // 개인/단체 토글 버튼 - 검색어가 비어있을 때만 작동
        binding.personalToggle.setOnClickListener {
            if (binding.searchEditText.text.toString().trim().isEmpty()) {
                viewModel.loadChatList(true)
            }
        }

        binding.groupToggle.setOnClickListener {
            if (binding.searchEditText.text.toString().trim().isEmpty()) {
                viewModel.loadChatList(false)
            }
        }
    }

    // 사용자 아이템 클릭 처리
    private fun handleUserItemClick(chatItem: ChatListItem) {
        // 로딩 다이얼로그 표시
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setMessage("채팅방을 확인하는 중...")
            .setCancelable(false)
            .create()

        loadingDialog.show()

        // 사용자 ID 추출
        val userId = chatItem.id.toLongOrNull()
        if (userId == null) {
            loadingDialog.dismiss()
            Toast.makeText(requireContext(), "잘못된 사용자 정보입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 코루틴 스코프 생성
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. 기존 채팅방 목록 가져오기
                val chatRooms = viewModel.repository.getChatRooms()

                // 2. 해당 사용자와의 개인 채팅방이 있는지 확인
                val existingChatRoom = chatRooms.find { chatRoom ->
                    !chatRoom.isGroup && chatRoom.name == chatItem.name
                }

                if (existingChatRoom != null) {
                    // 3a. 기존 채팅방이 있으면 해당 채팅방으로 이동
                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        navigateToChatRoom(existingChatRoom)
                    }
                } else {
                    // 3b. 기존 채팅방이 없으면 새 채팅방 생성
                    val apiService = MaiteRetrofitClient.getInstance(requireContext())
                    val createRequest = CreateChatRoomRequest(receiverId = userId)
                    val response = apiService.createChatRoom(createRequest)

                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()

                        if (response.isSuccessful && response.body()?.isSuccess == true) {
                            // 생성된 채팅방 정보로 ChatListItem 생성
                            val result = response.body()?.result
                            val newChatItem = ChatListItem(
                                id = result?.id.toString(),
                                name = result?.roomName ?: chatItem.name,
                                profileImageUrl = result?.profileImageUrl ?: chatItem.profileImageUrl,
                                lastMessage = result?.lastMessageContent,
                                intro = null,
                                timestamp = System.currentTimeMillis(),
                                isGroup = false,
                                isUser = false // 이제 채팅방으로 처리
                            )
                            // 생성된 채팅방으로 이동
                            navigateToChatRoom(newChatItem)

                            // 채팅 목록 새로고침
                            viewModel.refreshChatList()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "채팅방을 생성할 수 없습니다: ${response.body()?.message ?: "서버 오류"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "오류가 발생했습니다: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // 채팅방으로 이동
    private fun navigateToChatRoom(chatItem: ChatListItem) {
        val chatRoomFragment = ChatRoomFragment.newInstance(chatItem)
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                0,
                0,
                R.anim.slide_out_right
            )
            .add(R.id.main_frm, chatRoomFragment, ChatRoomFragment.TAG)
            .addToBackStack(null)
            .commit()
    }

    private fun observeViewModel() {
        // 채팅 목록 관찰
        viewModel.chatItems.observe(viewLifecycleOwner) { chatItems ->
            chatAdapter.submitList(chatItems)
        }

        // 로딩 상태 관찰
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // 로딩 표시 처리
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