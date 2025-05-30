package com.example.maite.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.maite.R
import com.example.maite.databinding.ItemPropMeetBinding
import com.example.maite.model.PropMeetItem

interface PropMeetClickListener {
    fun onAcceptClick(item: PropMeetItem)
    fun onRejectClick(item: PropMeetItem)
}

class PropMeetAdapter(private val clickListener: PropMeetClickListener) :
    ListAdapter<PropMeetItem, PropMeetAdapter.PropMeetViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropMeetViewHolder {
        val binding = ItemPropMeetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PropMeetViewHolder(binding, clickListener)
    }

    override fun onBindViewHolder(holder: PropMeetViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class PropMeetViewHolder(
        private val binding: ItemPropMeetBinding,
        private val clickListener: PropMeetClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PropMeetItem) {
            binding.propTitle.text = item.title
            binding.propDate.text = item.date
            binding.propTime.text = item.time
            binding.propPlace.text = item.place

            // acceptance 상태에 따른 UI 처리 추가
            when (item.acceptance.uppercase()) {
                "ACCEPTED" -> {
                    // 수락된 회의: 수락/거절 버튼 숨김, 상태 표시
                    binding.acceptBtn.visibility = View.GONE
                    binding.rejectBtn.visibility = View.GONE

                    // 상태 카드 표시
                    binding.status.visibility = View.VISIBLE
                    binding.statusBackground.setBackgroundResource(R.color.mainColor)
                    binding.statusText.setTextColor(ContextCompat.getColor(binding.root.context, R.color.white))
                    binding.statusText.text = "수락됨"
                }
                "REJECTED" -> {
                    // 거절된 회의: 수락/거절 버튼 숨김, 상태 표시
                    binding.acceptBtn.visibility = View.GONE
                    binding.rejectBtn.visibility = View.GONE

                    // 상태 카드 표시
                    binding.status.visibility = View.VISIBLE
                    binding.statusBackground.setBackgroundResource(R.color.light_gray)
                    binding.statusText.setTextColor(ContextCompat.getColor(binding.root.context, R.color.black))
                    binding.statusText.text = "거절됨"
                }
                else -> {
                    // PENDING 또는 다른 상태: 수락/거절 버튼 표시, 상태 숨김
                    binding.acceptBtn.visibility = View.VISIBLE
                    binding.rejectBtn.visibility = View.VISIBLE
                    binding.status.visibility = View.GONE

                    // 버튼 클릭 리스너 설정
                    binding.acceptBtn.setOnClickListener {
                        clickListener.onAcceptClick(item)
                    }

                    binding.rejectBtn.setOnClickListener {
                        clickListener.onRejectClick(item)
                    }
                }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PropMeetItem>() {
        override fun areItemsTheSame(oldItem: PropMeetItem, newItem: PropMeetItem): Boolean {
            return oldItem.title == newItem.title && oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: PropMeetItem, newItem: PropMeetItem): Boolean {
            return oldItem == newItem
        }
    }
}