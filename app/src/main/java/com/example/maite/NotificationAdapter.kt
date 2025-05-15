package com.example.maite.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.recyclerview.widget.RecyclerView
import com.example.maite.R
import com.example.maite.model.NotificationItem
import com.example.maite.model.NotificationType

class NotificationAdapter(
    private val notifications: List<NotificationItem>,
    private val onAcceptClick: (NotificationItem) -> Unit,
    private val onDeclineClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgProfile: ImageView = view.findViewById(R.id.img_profile)
        val tvSenderName: TextView = view.findViewById(R.id.tv_sender_name)
        val tvMessage: TextView = view.findViewById(R.id.tv_message)
        val btnAccept: MaterialButton = view.findViewById(R.id.btn_accept)
        val btnDecline: MaterialButton = view.findViewById(R.id.btn_decline)
        val buttonsLayout: ViewGroup = view.findViewById(R.id.buttons_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        
        holder.imgProfile.setImageResource(notification.profileImageRes)
        holder.tvSenderName.text = notification.senderName
        holder.tvMessage.text = notification.message
        
        // 알림 타입에 따라 버튼 표시 조정
        when (notification.type) {
            NotificationType.ROOM_INVITE,
            NotificationType.MEETING_INVITE -> {
                holder.buttonsLayout.visibility = View.VISIBLE
                holder.btnAccept.text = "수락"
                holder.btnDecline.text = "거절"
                
                holder.btnAccept.setOnClickListener {
                    onAcceptClick(notification)
                }
                
                holder.btnDecline.setOnClickListener {
                    onDeclineClick(notification)
                }
            }
            else -> {
                // 친구 요청이나 채팅 알림은 현재 미구현
                holder.buttonsLayout.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = notifications.size
}