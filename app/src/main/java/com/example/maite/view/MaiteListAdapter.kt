package com.example.maite.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.maite.databinding.ItemMaiteListBinding
import com.example.maite.model.MaiteListItem

class MaiteListAdapter(private val onClick: (MaiteListItem) -> Unit) : ListAdapter<MaiteListItem, MaiteListAdapter.MaiteViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MaiteViewHolder {
        return MaiteViewHolder(
            ItemMaiteListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onClick
        )
    }

    override fun onBindViewHolder(holder: MaiteViewHolder, position: Int) {
        val maiteListItem = getItem(position)
        holder.bind(maiteListItem)
    }

    class MaiteViewHolder(
        private val binding: ItemMaiteListBinding,
        private val onClick: (MaiteListItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(maiteListItem: MaiteListItem) {
            binding.title.text = maiteListItem.title
            binding.name.text = maiteListItem.name
            binding.intro.text = maiteListItem.intro
            binding.root.setOnClickListener {
                onClick(maiteListItem)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<MaiteListItem>() {
        override fun areItemsTheSame(oldItem: MaiteListItem, newItem: MaiteListItem): Boolean {
            // 지금은 고유 ID가 없다고 가정하고 전체 항목을 비교합니다
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: MaiteListItem, newItem: MaiteListItem): Boolean {
            return oldItem == newItem
        }
    }
}