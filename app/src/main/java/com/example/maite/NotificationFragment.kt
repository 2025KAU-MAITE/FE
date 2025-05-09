package com.example.maite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
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

        // 부드러운 애니메이션 적용
        binding.notificationPanel.translationX = 320f
        binding.notificationPanel.animate()
            .translationX(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 닫기 버튼
        binding.btnClose.setOnClickListener {
            closeWithAnimation()
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
            else -> {}
        }
    }

    private fun closeWithAnimation() {
        binding.notificationPanel.animate()
            .translationX(320f)
            .setDuration(300)
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