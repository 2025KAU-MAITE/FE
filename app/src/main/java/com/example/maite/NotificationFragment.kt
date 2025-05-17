package com.example.maite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import android.widget.TextView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.maite.adapter.NotificationAdapter
import com.example.maite.databinding.FragmentNotificationBinding
import com.example.maite.model.NotificationItem
import com.example.maite.model.NotificationType
import com.example.maite.notification.NotificationViewModel
import com.example.maite.notification.NotificationViewModelFactory

class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: NotificationViewModel by viewModels {
        NotificationViewModelFactory(requireContext())
    }
    
    private lateinit var invitesAdapter: NotificationAdapter
    private lateinit var proposalsAdapter: NotificationAdapter
    
    // 동적으로 생성된 친구 요청 섹션 뷰 참조를 위한 확장 속성
    private val FragmentNotificationBinding.sectionFriendRequests: View?
        get() = root.getTag(R.id.section_friend_requests) as? View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 시스템 상태바 높이를 고려한 패딩 설정
        binding.notificationPanel.apply {
            val statusBarHeight = getStatusBarHeight()
            // 상태바 높이의 2/3만 적용하여 여백 최소화
            setPadding(paddingLeft, statusBarHeight * 2 / 3, paddingRight, paddingBottom)
        }

        // 초기 설정 - 화면 밖으로 이동 및 투명하게 설정
        binding.dimBackground.alpha = 0f
        
        // 레이아웃이 완료된 후 애니메이션 실행
        binding.notificationPanel.post {
            // 패널의 너비를 구한 후 초기 위치 설정
            val panelWidth = binding.notificationPanel.width
            binding.notificationPanel.translationX = panelWidth.toFloat()
            
            // 배경 페이드인 애니메이션
            binding.dimBackground.animate()
                .alpha(1f)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .start()
            
            // 패널 슬라이드인 애니메이션
            binding.notificationPanel.animate()
                .translationX(0f)
                .setDuration(300)
                .setStartDelay(100) // 배경이 어느정도 나타난 후 슬라이드 시작
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // 닫기 버튼
        binding.btnClose.setOnClickListener {
            closeWithAnimation()
        }
        
        // 배경 클릭 시 닫기
        binding.dimBackground.setOnClickListener {
            closeWithAnimation()
        }
        
        // 패널 클릭 시 이벤트 전파 방지
        binding.notificationPanel.setOnClickListener {
            // 패널 내부를 클릭해도 닫히지 않도록 함
        }

        // RecyclerView 초기화
        setupRecyclerViews()
        
        // ViewModel 관찰
        observeViewModel()
        
        // 알림 데이터 로드
        viewModel.loadNotifications()
    }

    private fun setupRecyclerViews() {
        // 받은 초대 RecyclerView
        invitesAdapter = NotificationAdapter(
            emptyList(),
            onAcceptClick = { notification ->
                handleAccept(notification)
            },
            onDeclineClick = { notification ->
                handleDecline(notification)
            }
        )
        
        binding.rvInvites.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = invitesAdapter
        }
        
        // 약속 제안 RecyclerView
        proposalsAdapter = NotificationAdapter(
            emptyList(),
            onAcceptClick = { notification ->
                handleAccept(notification)
            },
            onDeclineClick = { notification ->
                handleDecline(notification)
            }
        )
        
        binding.rvProposals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = proposalsAdapter
        }
    }
    
    private fun observeViewModel() {
        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            // 알림 타입별로 분류
            val invites = notifications.filter { it.type == NotificationType.ROOM_INVITE }
            val proposals = notifications.filter { it.type == NotificationType.MEETING_INVITE }
            val friendRequests = notifications.filter { it.type == NotificationType.FRIEND_REQUEST }
            
            // 받은 초대 섹션
            if (invites.isNotEmpty()) {
                binding.sectionInvites.visibility = View.VISIBLE
                binding.rvInvites.adapter = NotificationAdapter(
                    invites,
                    onAcceptClick = { notification -> handleAccept(notification) },
                    onDeclineClick = { notification -> handleDecline(notification) }
                )
            } else {
                binding.sectionInvites.visibility = View.GONE
            }
            
            // 약속 제안 섹션
            if (proposals.isNotEmpty()) {
                binding.sectionProposals.visibility = View.VISIBLE
                binding.rvProposals.adapter = NotificationAdapter(
                    proposals,
                    onAcceptClick = { notification -> handleAccept(notification) },
                    onDeclineClick = { notification -> handleDecline(notification) }
                )
            } else {
                binding.sectionProposals.visibility = View.GONE
            }
            
            // 친구 요청 섹션 - 새로 추가
            if (friendRequests.isNotEmpty()) {
                // ScrollView 내부 LinearLayout 찾기 (binding.llNotifications 대신 다른 방법 사용)
                val scrollView = binding.notificationPanel.findViewById<LinearLayout>(R.id.ll_notifications)
                
                // 친구 요청 섹션이 없는 경우 동적으로 생성
                if (binding.sectionFriendRequests == null) {
                    // 섹션 템플릿 복사
                    val sectionInvitesOriginal = binding.sectionInvites
                    val sectionFriendRequests = layoutInflater.inflate(
                        R.layout.layout_notification_section,
                        scrollView, // binding.llNotifications 대신 직접 찾은 LinearLayout 사용
                        false
                    )
                    
                    // 섹션 제목 설정
                    val tvSectionTitle = sectionFriendRequests.findViewById<TextView>(R.id.tv_section_title)
                    tvSectionTitle.text = "친구 요청"
                    
                    // RecyclerView 참조 가져오기
                    val rvFriendRequests = sectionFriendRequests.findViewById<RecyclerView>(R.id.rv_notifications)
                    
                    // RecyclerView 설정
                    rvFriendRequests.layoutManager = LinearLayoutManager(requireContext())
                    rvFriendRequests.adapter = NotificationAdapter(
                        friendRequests,
                        onAcceptClick = { notification -> handleAccept(notification) },
                        onDeclineClick = { notification -> handleDecline(notification) }
                    )
                    
                    // 레이아웃에 추가 (원하는 위치에 배치 - 예: 제안 섹션의 상단)
                    val indexToInsert = scrollView.indexOfChild(binding.sectionProposals) + 1
                    scrollView.addView(sectionFriendRequests, indexToInsert)
                    
                    // 생성된 레이아웃 객체 저장 (허용된 메터로 리소스 ID 사용)
                    binding.root.setTag(R.id.section_friend_requests, sectionFriendRequests)
                } else {
                    // 이미 존재하는 경우 화면에 보이게 설정
                    binding.sectionFriendRequests?.visibility = View.VISIBLE
                    
                    // RecyclerView 업데이트
                    val rvFriendRequests = binding.sectionFriendRequests?.findViewById<RecyclerView>(R.id.rv_notifications)
                    rvFriendRequests?.adapter = NotificationAdapter(
                        friendRequests,
                        onAcceptClick = { notification -> handleAccept(notification) },
                        onDeclineClick = { notification -> handleDecline(notification) }
                    )
                }
            } else {
                // 친구 요청이 없는 경우 섹션 숨기기
                binding.sectionFriendRequests?.visibility = View.GONE
            }
            
            // 알림이 없을 때 메시지
            if (notifications.isEmpty()) {
                binding.tvNoNotifications.visibility = View.VISIBLE
            } else {
                binding.tvNoNotifications.visibility = View.GONE
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleAccept(notification: NotificationItem) {
        when (notification.type) {
            NotificationType.ROOM_INVITE -> {
                viewModel.acceptRoomInvite(notification.id)
                Toast.makeText(requireContext(), "회의방에 참가했습니다.", Toast.LENGTH_SHORT).show()
            }
            NotificationType.MEETING_INVITE -> {
                viewModel.acceptMeetingProposal(notification.id)
                Toast.makeText(requireContext(), "회의 제안을 수락했습니다.", Toast.LENGTH_SHORT).show()
            }
            NotificationType.FRIEND_REQUEST -> {
                viewModel.acceptFriendRequest(notification.id)
                Toast.makeText(requireContext(), "${notification.senderName}님과 친구가 되었습니다.", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }
    
    private fun handleDecline(notification: NotificationItem) {
        when (notification.type) {
            NotificationType.ROOM_INVITE -> {
                viewModel.declineRoomInvite(notification.id)
                Toast.makeText(requireContext(), "회의방 초대를 거절했습니다.", Toast.LENGTH_SHORT).show()
            }
            NotificationType.MEETING_INVITE -> {
                viewModel.declineMeetingProposal(notification.id)
                Toast.makeText(requireContext(), "회의 제안을 거절했습니다.", Toast.LENGTH_SHORT).show()
            }
            NotificationType.FRIEND_REQUEST -> {
                viewModel.declineFriendRequest(notification.id)
                Toast.makeText(requireContext(), "${notification.senderName}님의 친구 요청을 거절했습니다.", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    private fun closeWithAnimation() {
        // 패널 슬라이드 아웃
        binding.notificationPanel.animate()
            .translationX(binding.notificationPanel.width.toFloat())
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
        
        // 배경 페이드 아웃
        binding.dimBackground.animate()
            .alpha(0f)
            .setDuration(200)
            .setStartDelay(100) // 패널이 어느정도 나가기 시작한 후 페이드 아웃
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                parentFragmentManager.beginTransaction()
                    .remove(this@NotificationFragment)
                    .commit()
            }
            .start()
    }
    
    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "NotificationFragment"
    }
}