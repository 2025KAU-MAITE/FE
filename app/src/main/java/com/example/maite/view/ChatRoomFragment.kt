package com.example.maite.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.maite.databinding.FragmentChatRoomBinding
import com.example.maite.model.ChatListItem
import com.example.maite.model.ChatRoomRepository
import com.example.maite.viewmodel.ChatRoomViewModel
import com.example.maite.viewmodel.ChatRoomViewModelFactory
import com.bumptech.glide.Glide
import com.example.maite.R

class ChatRoomFragment : Fragment() {

    companion object {
        const val TAG = "ChatRoomFragment"
        private const val ARG_CHAT_ITEM = "chat_item"

        fun newInstance(chatItem: ChatListItem): ChatRoomFragment {
            val fragment = ChatRoomFragment()
            val args = Bundle()
            args.putParcelable(ARG_CHAT_ITEM, chatItem)
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentChatRoomBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ChatRoomViewModel
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var chatItem: ChatListItem

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

        chatItem = arguments?.getParcelable(ARG_CHAT_ITEM)
            ?: throw IllegalArgumentException("ChatListItem이 필요합니다")

        // 즉시 UI 설정 (API 호출 없이)
        setupChatRoomUI()

        // ViewModel 초기화 - Context 전달
        val repository = ChatRoomRepository(requireContext())
        val factory = ChatRoomViewModelFactory(chatItem.id, repository)
        viewModel = ViewModelProvider(this, factory)[ChatRoomViewModel::class.java]

        setupUI()
        observeViewModel()
    }

    private fun setupChatRoomUI() {
        // ChatListItem의 정보로 즉시 UI 설정
        binding.title.text = if (chatItem.isGroup) {
            "${chatItem.name}의 MAITE"
        } else {
            chatItem.name
        }

        // 프로필 이미지 설정
        chatItem.profileImageUrl?.let { imageUrl ->
            Glide.with(requireContext())
                .load(imageUrl)
                .placeholder(R.drawable.img_profile_default)
                .error(R.drawable.img_profile_default)
                .into(binding.profileImg)
        } ?: run {
            binding.profileImg.setImageResource(R.drawable.img_profile_default)
        }
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
        // 메시지 목록 관찰만 유지 (채팅방 정보는 이미 ChatListItem에서 가져옴)
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            messageAdapter.submitList(messages) {
                if (messages.isNotEmpty()) {
                    binding.messageRv.smoothScrollToPosition(messages.size - 1)
                }
            }
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}