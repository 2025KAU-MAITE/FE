package com.example.maite.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.maite.databinding.ItemMeetListBinding // 생성된 바인딩 클래스 import
import com.example.maite.model.MeetListItem // 데이터 모델 import

class MeetListAdapter(private val onItemClicked: (MeetListItem) -> Unit) :
    ListAdapter<MeetListItem, MeetListAdapter.MeetViewHolder>(MeetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeetViewHolder {
        val binding = ItemMeetListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MeetViewHolder(binding, onItemClicked)
    }

    override fun onBindViewHolder(holder: MeetViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class MeetViewHolder(
        private val binding: ItemMeetListBinding,
        private val onItemClicked: (MeetListItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MeetListItem) {
            binding.meetTitle.text = item.title
            binding.meetDate.text = item.date
            binding.meetTime.text = "${item.time} - ${item.endTime}"
            binding.meetPlace.text = item.place

            // 아이템 전체 클릭 리스너 설정 (필요하다면)
            binding.root.setOnClickListener {
                onItemClicked(item)
            }
        }
    }

    // DiffUtil 콜백 클래스 (id 비교 제거됨)
    class MeetDiffCallback : DiffUtil.ItemCallback<MeetListItem>() {
        override fun areItemsTheSame(oldItem: MeetListItem, newItem: MeetListItem): Boolean {
            // ID가 없으므로, 내용이 완전히 같으면 같은 아이템으로 간주하거나,
            // title과 date 등 핵심 정보가 같으면 같은 아이템으로 간주할 수 있습니다.
            // 여기서는 간단히 내용 전체를 비교합니다.
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: MeetListItem, newItem: MeetListItem): Boolean {
            return oldItem == newItem // 데이터 클래스의 내용 전체 비교
        }
    }
}