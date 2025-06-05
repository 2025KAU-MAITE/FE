package com.example.maite.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.maite.R
import com.example.maite.model.ChatListItem

class ChatListAdapter(
    private val onItemClick: (ChatListItem) -> Unit
) : ListAdapter<ChatListItem, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_PERSONAL = 1
        private const val VIEW_TYPE_GROUP = 2
    }

    // 개인 채팅 ViewHolder
    class PersonalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImg: ImageView = view.findViewById(R.id.profileImg)
        val nameText: TextView = view.findViewById(R.id.name)
    }

    // 단체 채팅 ViewHolder
    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImg: ImageView = view.findViewById(R.id.profileImg)
        val nameText: TextView = view.findViewById(R.id.name)
        val introText: TextView = view.findViewById(R.id.intro)
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        // 사용자 아이템은 개인 채팅 아이템 레이아웃 사용
        return if (item.isGroup) VIEW_TYPE_GROUP else VIEW_TYPE_PERSONAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_PERSONAL -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_personal, parent, false)
                PersonalViewHolder(view)
            }
            VIEW_TYPE_GROUP -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_maite, parent, false)
                GroupViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chatItem = getItem(position)

        when (holder) {
            is PersonalViewHolder -> {
                // 개인 채팅 아이템 또는 사용자 아이템 바인딩
                holder.nameText.text = if (chatItem.isUser) {
                    // 사용자 아이템인 경우, 이름과 이메일을 함께 표시
                    "${chatItem.name} (${chatItem.intro})"
                } else {
                    // 일반 개인 채팅 아이템
                    chatItem.name
                }

                // 아이템 클릭 리스너 설정
                holder.itemView.setOnClickListener {
                    onItemClick(chatItem)
                }
            }
            is GroupViewHolder -> {
                // 단체 채팅 아이템 바인딩
                holder.nameText.text = chatItem.name
                holder.introText.text = chatItem.intro ?: "소개글이 없습니다."

                // 아이템 클릭 리스너 설정
                holder.itemView.setOnClickListener {
                    onItemClick(chatItem)
                }
            }
        }
    }

    // DiffUtil을 통한 효율적인 아이템 업데이트
    class ChatDiffCallback : DiffUtil.ItemCallback<ChatListItem>() {
        override fun areItemsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
            return oldItem.id == newItem.id && oldItem.isUser == newItem.isUser
        }

        override fun areContentsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
            return oldItem == newItem
        }
    }
}