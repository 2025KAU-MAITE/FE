package com.example.maite.view // 실제 패키지명으로

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat // ContextCompat import 추가
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.maite.R // R 클래스 import 추가
import com.example.maite.databinding.ItemPropMeetBinding // 실제 바인딩 클래스명으로
import com.example.maite.model.PropMeetItem

interface PropMeetClickListener {
    fun onAcceptClick(item: PropMeetItem)
    fun onRejectClick(item: PropMeetItem)
}

class PropMeetAdapter(
    private val buttonClickListener: PropMeetClickListener, // 수락/거절 버튼 리스너
    private val itemClickListener: (PropMeetItem) -> Unit // 아이템 전체 클릭 리스너
) : ListAdapter<PropMeetItem, PropMeetAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPropMeetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, buttonClickListener, itemClickListener)
    }

    class ViewHolder(private val binding: ItemPropMeetBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: PropMeetItem,
            buttonClickListener: PropMeetClickListener,
            itemClickListener: (PropMeetItem) -> Unit
        ) {
            binding.propTitle.text = item.title
            binding.propDate.text = item.date
            binding.propTime.text = "${item.time} - ${item.endTime}"
            binding.propPlace.text = item.place


            // 아이템 전체 뷰에 대한 클릭 리스너 설정
            binding.root.setOnClickListener {
                itemClickListener(item)
            }

            // acceptance 상태에 따른 UI 처리 (ListDetailFragment와 동일하게)
            val acceptanceStatus = item.acceptance?.uppercase() ?: "PENDING" // Null-safe uppercase

            when (acceptanceStatus) {
                "ACCEPTED" -> {
                    binding.acceptBtn.visibility = View.GONE
                    binding.rejectBtn.visibility = View.GONE
                    binding.status.visibility = View.VISIBLE
                    binding.statusBackground.setBackgroundResource(R.color.mainColor) // mainColor가 colors.xml에 정의되어 있어야 함
                    binding.statusText.setTextColor(ContextCompat.getColor(binding.root.context, R.color.white))
                    binding.statusText.text = "수락됨"
                }
                "REJECTED" -> {
                    binding.acceptBtn.visibility = View.GONE
                    binding.rejectBtn.visibility = View.GONE
                    binding.status.visibility = View.VISIBLE
                    binding.statusBackground.setBackgroundResource(R.color.light_gray) // light_gray가 colors.xml에 정의되어 있어야 함
                    binding.statusText.setTextColor(ContextCompat.getColor(binding.root.context, R.color.black))
                    binding.statusText.text = "거절됨"
                }
                else -> { // "PENDING" 또는 기타 상태
                    binding.acceptBtn.visibility = View.VISIBLE
                    binding.rejectBtn.visibility = View.VISIBLE
                    binding.status.visibility = View.GONE

                    // 버튼 클릭 리스너 설정
                    binding.acceptBtn.setOnClickListener {
                        buttonClickListener.onAcceptClick(item)
                    }
                    binding.rejectBtn.setOnClickListener {
                        buttonClickListener.onRejectClick(item)
                    }
                }
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PropMeetItem>() {
        override fun areItemsTheSame(oldItem: PropMeetItem, newItem: PropMeetItem): Boolean {
            return oldItem.meetingId == newItem.meetingId
        }

        override fun areContentsTheSame(oldItem: PropMeetItem, newItem: PropMeetItem): Boolean {
            return oldItem == newItem
        }
    }
}