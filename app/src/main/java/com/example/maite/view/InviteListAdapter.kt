package com.example.maite.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.maite.databinding.ItemInviteListBinding
import com.example.maite.model.InviteListItem

class InviteListAdapter(
    // 선택 상태 변경을 알릴 콜백 추가
    private val onSelectionChanged: ((Boolean) -> Unit)? = null
) : ListAdapter<InviteListItem, InviteListAdapter.InviteViewHolder>(DiffCallback) {

    // 선택된 항목들을 저장하는 집합(Set) 사용
    private val selectedItems = HashSet<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteViewHolder {
        return InviteViewHolder(
            ItemInviteListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: InviteViewHolder, position: Int) {
        val inviteListItem = getItem(position)
        holder.bind(inviteListItem, selectedItems.contains(position))

        // 아이템 클릭 이벤트 처리
        holder.itemView.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                // 토글 기능 구현: 이미 선택된 항목이면 선택 해제, 아니면 선택
                if (selectedItems.contains(adapterPosition)) {
                    selectedItems.remove(adapterPosition)
                } else {
                    selectedItems.add(adapterPosition)
                }
                notifyItemChanged(adapterPosition)

                // 선택 상태 변경 콜백 호출
                onSelectionChanged?.invoke(selectedItems.isNotEmpty())
            }
        }
    }

    class InviteViewHolder(private val binding: ItemInviteListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(inviteListItem: InviteListItem, isSelected: Boolean) {
            binding.name.text = inviteListItem.name

            // MaterialCardView의 선택 상태 설정
            binding.inviteCardView.isSelected = isSelected
        }
    }

    // 선택된 아이템들의 위치를 반환하는 메서드 (필요 시 사용)
    fun getSelectedItemPositions(): Set<Int> = selectedItems.toSet()

    // 선택된 아이템들을 가져오는 메서드 (필요 시 사용)
    fun getSelectedItems(): List<InviteListItem> {
        return selectedItems.mapNotNull { position ->
            if (position < itemCount) getItem(position) else null
        }
    }

    // 선택 상태 모두 초기화하는 메서드 (필요 시 사용)
    fun clearSelections() {
        val previousSelected = HashSet(selectedItems)
        selectedItems.clear()
        previousSelected.forEach { notifyItemChanged(it) }

        // 선택 상태 변경 콜백 호출
        onSelectionChanged?.invoke(false)
    }

    // 현재 선택된 항목이 있는지 확인
    fun hasSelectedItems(): Boolean = selectedItems.isNotEmpty()

    companion object DiffCallback : DiffUtil.ItemCallback<InviteListItem>() {
        override fun areItemsTheSame(oldItem: InviteListItem, newItem: InviteListItem): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: InviteListItem, newItem: InviteListItem): Boolean {
            return oldItem == newItem
        }
    }
}