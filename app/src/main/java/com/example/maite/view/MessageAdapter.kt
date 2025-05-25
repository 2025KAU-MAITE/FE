package com.example.maite.view

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.maite.R
import com.example.maite.model.Message
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MessageAdapter(private val currentUserId: String) :
    ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
                ReceivedMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    // 리스트가 변경될 때마다 호출되는 submitList 메서드 오버라이드
    override fun submitList(list: List<Message>?) {
        Log.d("MessageAdapter", "메시지 목록 업데이트: ${list?.size}개")
        if (list != null && list.isNotEmpty()) {
            list.forEachIndexed { index, message ->
                Log.d("MessageAdapter", "[$index] id=${message.id}, content=${message.content}, senderId=${message.senderId}")
            }
        }
        super.submitList(list)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)

        Log.d("MessageAdapter", "메시지 표시: id=${message.id}, 내용=${message.content}, 타입=${getItemViewType(position)}")

        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> {
                // 이전 메시지와 같은 사람이 보낸 메시지인지 확인
                val showSender = position == 0 ||
                        getItem(position - 1).senderId != message.senderId
                holder.bind(message, showSender)
            }
        }
    }

    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.tvMessageContent)
        private val timeText: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(message: Message) {
            messageText.text = message.content
            timeText.text = formatTime(message.timestamp)
        }
    }

    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val senderText: TextView = itemView.findViewById(R.id.tvSenderName)
        private val messageText: TextView = itemView.findViewById(R.id.tvMessageContent)
        private val timeText: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(message: Message, showSender: Boolean) {
            messageText.text = message.content
            timeText.text = formatTime(message.timestamp)

            if (showSender) {
                senderText.visibility = View.VISIBLE
                senderText.text = message.senderName
            } else {
                senderText.visibility = View.GONE
            }
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private fun formatTime(timestamp: Long): String {
            // 명시적으로 로컬 시간대 설정
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp

            // 디버그용 로그
            Log.d("TimeDebug", "원본 타임스탬프: $timestamp")
            Log.d("TimeDebug", "UTC 시간: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date(timestamp))}")
            Log.d("TimeDebug", "로컬 시간: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))}")

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            // 명시적으로 타임존 설정 (시스템 기본값)
            sdf.timeZone = TimeZone.getDefault()

            return sdf.format(Date(timestamp))
        }

        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
}