package com.example.maite.view

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
import java.util.Date
import java.util.Locale

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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)

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
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }
}