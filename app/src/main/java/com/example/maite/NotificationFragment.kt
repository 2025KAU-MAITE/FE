package com.example.maite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.maite.adapter.NotificationAdapter
import com.example.maite.databinding.FragmentNotificationBinding
import com.example.maite.model.NotificationItem
import com.example.maite.model.NotificationType

class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!

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

        // 알림 리스트 세팅
        setupNotificationList()
    }

    private fun setupNotificationList() {
        val dummyNotifications = listOf(
            NotificationItem(
                id = 1,
                type = NotificationType.MEETING_INVITE,
                senderName = "김정훈의 MAITE",
                message = "\"김정훈의 MAITE\"에서 제안을 받았어요.",
                profileImageRes = R.drawable.ic_launcher_foreground
            ),
            NotificationItem(
                id = 2,
                type = NotificationType.FRIEND_REQUEST,
                senderName = "김정훈의 MAITE",
                message = "\"김정훈의 MAITE\"님이 친구 요청을 보냈습니다.",
                profileImageRes = R.drawable.ic_launcher_foreground
            ),
            NotificationItem(
                id = 3,
                type = NotificationType.CHAT,
                senderName = "김정훈의 MAITE",
                message = "\"김정훈의 MAITE\"와의 채팅방에 새 메시지가 도착했습니다.",
                profileImageRes = R.drawable.ic_launcher_foreground
            )
        )

        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = NotificationAdapter(dummyNotifications)
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