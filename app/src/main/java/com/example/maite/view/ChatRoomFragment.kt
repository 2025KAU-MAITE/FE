package com.example.maite.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.maite.databinding.FragmentChatRoomBinding
import com.example.maite.model.ChatRoomRepository
import com.example.maite.viewmodel.ChatRoomViewModel
import com.example.maite.viewmodel.ChatRoomViewModelFactory

class ChatRoomFragment : Fragment() {

    companion object {
        const val TAG = "ChatRoomFragment"
        private const val ARG_CHAT_ID = "chat_id"

        fun newInstance(chatId: String): ChatRoomFragment {
            val fragment = ChatRoomFragment()
            val args = Bundle()
            args.putString(ARG_CHAT_ID, chatId)
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentChatRoomBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChatRoomViewModel
    private lateinit var messageAdapter: MessageAdapter

    private val currentUserId = "current_user_id" // 실제로는 로그인 유저 ID 사용

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatRoomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chatId = arguments?.getString(ARG_CHAT_ID)
            ?: throw IllegalArgumentException("채팅 ID가 필요합니다")

        // ViewModel 초기화
        val repository = ChatRoomRepository()
        val factory = ChatRoomViewModelFactory(chatId, repository)
        viewModel = ViewModelProvider(this, factory)[ChatRoomViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        // 메시지 어댑터 설정
        messageAdapter = MessageAdapter(currentUserId)

        binding.messageRv.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true // 하단부터 쌓기
            }
            adapter = messageAdapter
        }

        // 뒤로 가기 버튼
        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 메시지 전송
        binding.sendBtn.setOnClickListener {
            val messageContent = binding.etMessageInput.text.toString()
            if (messageContent.isNotBlank()) {
                viewModel.sendMessage(messageContent)
                binding.etMessageInput.text.clear()
            }
        }

        // 키보드에서 전송 버튼 처리
        binding.etMessageInput.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val messageContent = textView.text.toString()
                if (messageContent.isNotBlank()) {
                    viewModel.sendMessage(messageContent)
                    binding.etMessageInput.text.clear()
                }
                true
            } else {
                false
            }
        }
    }

    private fun observeViewModel() {
        // 채팅방 정보 관찰
        viewModel.chatRoom.observe(viewLifecycleOwner) { chatRoom ->
            binding.title.text = if (chatRoom.isGroup) {
                "${chatRoom.name}의 MAITE"
            } else {
                chatRoom.name
            }

            // 프로필 이미지 설정 (필요하다면 Glide 등으로 이미지 로드)
        }

        // 메시지 목록 관찰
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            messageAdapter.submitList(messages) {
                if (messages.isNotEmpty()) {
                    binding.messageRv.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        // 로딩 상태 관찰
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // 로딩 표시 처리 (필요한 경우 ProgressBar 추가)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}