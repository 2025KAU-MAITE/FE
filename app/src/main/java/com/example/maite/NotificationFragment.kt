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
    
    private lateinit var notificationAdapter: NotificationAdapter

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

        // 부드러운 애니메이션 적용
        binding.notificationPanel.translationX = 320f // 패널 너비
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
        setupRecyclerView()
        
        // ViewModel 관찰
        observeViewModel()
        
        // 알림 데이터 로드
        viewModel.loadNotifications()
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            emptyList(),
            onAcceptClick = { notification ->
                handleAccept(notification)
            },
            onDeclineClick = { notification ->
                handleDecline(notification)
            }
        )
        
        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
        }
    }
    
    private fun observeViewModel() {
        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            // 알림 타입별로 분류하여 표시 순서 조정
            val sortedNotifications = notifications.sortedBy { notification ->
                when (notification.type) {
                    NotificationType.ROOM_INVITE -> 0
                    NotificationType.MEETING_INVITE -> 1
                    NotificationType.FRIEND_REQUEST -> 2
                    NotificationType.CHAT -> 3
                }
            }
            
            // 어댑터 갱신 (새로운 인스턴스 생성)
            binding.rvNotifications.adapter = NotificationAdapter(
                sortedNotifications,
                onAcceptClick = { notification -> handleAccept(notification) },
                onDeclineClick = { notification -> handleDecline(notification) }
            )
            
            // 알림이 없을 때 표시할 메시지
            if (notifications.isEmpty()) {
                binding.tvNoNotifications?.visibility = View.VISIBLE
                binding.rvNotifications.visibility = View.GONE
            } else {
                binding.tvNoNotifications?.visibility = View.GONE
                binding.rvNotifications.visibility = View.VISIBLE
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
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
                // TODO: 친구 요청 수락 API 구현 필요
                Toast.makeText(requireContext(), "친구 요청을 수락했습니다.", Toast.LENGTH_SHORT).show()
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
                // TODO: 친구 요청 거절 API 구현 필요
                Toast.makeText(requireContext(), "친구 요청을 거절했습니다.", Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "NotificationFragment"
    }
}