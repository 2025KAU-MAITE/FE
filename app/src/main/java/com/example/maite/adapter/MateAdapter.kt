package com.example.maite.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.maite.R
import com.example.maite.databinding.ItemMateBinding
import com.example.maite.model.MateItem

class MateAdapter(
    private val onDeleteClick: (MateItem) -> Unit
) : ListAdapter<MateItem, MateAdapter.MateViewHolder>(MateDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MateViewHolder {
        val binding = ItemMateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MateViewHolder(
        private val binding: ItemMateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // 삭제 버튼 클릭 이벤트
            binding.btnDelete.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }
        }

        fun bind(item: MateItem) {
            // 이름 설정
            binding.tvName.text = item.name
            
            // 이메일 설정 (없을 경우 빈 문자열)
            binding.tvEmail.text = item.email ?: ""
            
            // 프로필 이미지 설정
            if (!item.profileImageUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(item.profileImageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.img_profile_default)
                    .error(R.drawable.img_profile_default)
                    .into(binding.ivProfile)
            } else {
                // 기본 이미지 로드
                Glide.with(binding.root.context)
                    .load(R.drawable.img_profile_default)
                    .circleCrop()
                    .into(binding.ivProfile)
            }
        }
    }

    // DiffUtil 구현
    private class MateDiffCallback : DiffUtil.ItemCallback<MateItem>() {
        override fun areItemsTheSame(oldItem: MateItem, newItem: MateItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MateItem, newItem: MateItem): Boolean {
            return oldItem == newItem
        }
    }
}