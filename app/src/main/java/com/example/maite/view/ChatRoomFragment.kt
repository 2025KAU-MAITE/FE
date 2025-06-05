package com.example.maite.view

import android.os.Bundle
import android.util.Log
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
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.example.maite.MaiteApiService
import com.example.maite.MaiteRetrofitClient
import com.example.maite.R
import com.example.maite.PreferencesUtil
import com.example.maite.model.Message
import com.example.maite.model.MessageType
import com.example.maite.network.WebSocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.app.Activity
import android.provider.MediaStore

class ChatRoomFragment : Fragment() {

    companion object {
        const val TAG = "ChatRoomFragment"
        private const val ARG_CHAT_ITEM = "chat_item"
        private const val REQUEST_IMAGE_PICK = 1001

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
    private lateinit var preferencesUtil: PreferencesUtil
    private lateinit var apiService: MaiteApiService

    // 실제 로그인 유저 ID로 변경
    private val currentUserId: String
        get() = preferencesUtil.getUserId()?.toString() ?: "-1"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatRoomBinding.inflate(inflater, container, false)

        // PreferencesUtil 초기화
        preferencesUtil = PreferencesUtil(requireContext())
        apiService = MaiteRetrofitClient.getInstance(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatItem = arguments?.getParcelable(ARG_CHAT_ITEM)
            ?: throw IllegalArgumentException("ChatListItem이 필요합니다")

        // PreferencesUtil 초기화
        preferencesUtil = PreferencesUtil(requireContext())

        // 즉시 UI 설정 (API 호출 없이)
        setupChatRoomUI()

        // ViewModel 초기화 - Context 및 PreferencesUtil 전달
        val repository = ChatRoomRepository(requireContext())
        val factory = ChatRoomViewModelFactory(chatItem.id, repository, preferencesUtil)
        viewModel = ViewModelProvider(this, factory)[ChatRoomViewModel::class.java]

        setupUI()
        observeViewModel()

        // 상대방의 프로필 이미지 로드 (채팅방 이름으로 검색)
        if (!chatItem.isGroup) {
            loadReceiverProfileImage(chatItem.name)
        }

        // 로그 추가
        Log.d(TAG, "ChatRoomFragment 초기화 완료 - 채팅방 ID: ${chatItem.id}, 사용자 ID: $currentUserId")
    }

    private fun setupChatRoomUI() {
        // ChatListItem의 정보로 즉시 UI 설정
        binding.title.text = if (chatItem.isGroup) {
            "${chatItem.name}의 MAITE"
        } else {
            chatItem.name
        }

        // 프로필 이미지 설정 (캐시된 이미지가 있으면 사용)
        chatItem.profileImageUrl?.let { imageUrl ->
            Glide.with(requireContext())
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .skipMemoryCache(true)
                .signature(ObjectKey(System.currentTimeMillis().toString()))
                .placeholder(R.drawable.img_profile_default)
                .error(R.drawable.img_profile_default)
                .into(binding.profileImg)
        } ?: run {
            binding.profileImg.setImageResource(R.drawable.img_profile_default)
        }
    }

    private fun loadReceiverProfileImage(receiverName: String) {
        // 이미 프로필 URL이 있으면 사용
        if (!chatItem.profileImageUrl.isNullOrBlank()) {
            Glide.with(requireContext())
                .load(chatItem.profileImageUrl)
                .apply(RequestOptions.circleCropTransform())
                .skipMemoryCache(true)
                .signature(ObjectKey(System.currentTimeMillis().toString()))
                .placeholder(R.drawable.img_profile_default)
                .error(R.drawable.img_profile_default)
                .into(binding.profileImg)
            return
        }

        // 상대방의 이름(또는 이메일)으로 사용자 검색하여 프로필 이미지 가져오기
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.searchUsers(receiverName)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val userList = response.body()?.result
                    val matchingUser = userList?.firstOrNull {
                        it.name == receiverName || it.email.contains(receiverName, ignoreCase = true)
                    }

                    if (matchingUser != null && !matchingUser.profileImageUrl.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            Glide.with(requireContext())
                                .load(matchingUser.profileImageUrl)
                                .apply(RequestOptions.circleCropTransform())
                                .skipMemoryCache(true)
                                .signature(ObjectKey(System.currentTimeMillis().toString()))
                                .placeholder(R.drawable.img_profile_default)
                                .error(R.drawable.img_profile_default)
                                .into(binding.profileImg)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "프로필 이미지 로드 실패: $receiverName", e)
            }
        }
    }

    private fun setupUI() {
        // 메시지 어댑터 설정 (currentUserId는 이제 실제 로그인한 사용자 ID)
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

        // 이미지 버튼 클릭 시 이미지 선택 기능
        binding.imgBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, REQUEST_IMAGE_PICK)
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
        // 메시지 목록 관찰
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            Log.d(TAG, "메시지 목록 변경: ${messages.size}개")
            messageAdapter.submitList(messages) {
                if (messages.isNotEmpty()) {
                    binding.messageRv.post {
                        binding.messageRv.smoothScrollToPosition(messages.size - 1)
                    }
                }

                // 상대방의 메시지가 있으면 첫 번째 메시지에서 프로필 이미지 추출하여 설정
                messages.firstOrNull { it.senderId != currentUserId && !it.senderProfileImageUrl.isNullOrBlank() }?.let { message ->
                    if (!message.senderProfileImageUrl.isNullOrBlank()) {
                        Glide.with(requireContext())
                            .load(message.senderProfileImageUrl)
                            .apply(RequestOptions.circleCropTransform())
                            .skipMemoryCache(true)
                            .signature(ObjectKey(System.currentTimeMillis().toString()))
                            .placeholder(R.drawable.img_profile_default)
                            .error(R.drawable.img_profile_default)
                            .into(binding.profileImg)
                    }
                }
            }
        }

        // 에러 메시지 관찰
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "에러 메시지: $it")
                viewModel.clearError()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            // 이미지 전송 처리는 백엔드 구현에 따라 달라질 수 있으므로
            // 현재는 로그만 기록
            Log.d(TAG, "이미지 선택됨: ${data?.data}")

            // TODO: 실제 이미지 전송 구현
            Toast.makeText(requireContext(), "이미지 메시지는 현재 지원되지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}