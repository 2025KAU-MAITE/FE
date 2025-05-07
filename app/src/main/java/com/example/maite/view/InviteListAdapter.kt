package com.example.maite.view

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.maite.databinding.ItemInviteListBinding
import com.example.maite.model.InviteListItem

class InviteListAdapter(
    private val onSelectionChanged: ((Boolean, Boolean) -> Unit)? = null,
    private val isFromListDetail: Boolean = false // 추가된 파라미터
) : ListAdapter<InviteListItem, InviteListAdapter.InviteViewHolder>(DiffCallback) {

    // 선택된 항목들을 저장하는 집합(Set) 사용
    private val selectedItems = HashSet<Int>()

    // 초기 선택 상태를 저장
    private val initialSelectedItems = HashSet<Int>()

    // 변경 불가능한 항목 (ListDetailFragment에서 호출된 경우 초기 선택된 항목)
    private val immutableItems = HashSet<Int>()

    // 사전 선택된 사용자 ID 목록
    private var preSelectedUserIds = listOf<Long>()

    // 선택 상태가 변경되었는지 여부
    private var selectionChanged = false

    // 한 번 초기화 후 onBindViewHolder에서 다시 preSelect하지 않도록 플래그 설정
    private var initialSelectionApplied = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteViewHolder {
        val binding = ItemInviteListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InviteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InviteViewHolder, position: Int) {
        val currentItem = getItem(position)

        // 현재 선택 상태에 따라 UI 업데이트
        val isSelected = selectedItems.contains(position)

        // 직접 바인딩 설정
        holder.binding.name.text = currentItem.name
        holder.binding.inviteCardView.isSelected = isSelected

        // 클릭 리스너 설정
        holder.binding.root.setOnClickListener {
            toggleSelection(position)
        }

        holder.binding.inviteCardView.setOnClickListener {
            toggleSelection(position)
        }

        Log.d("InviteListAdapter", "Item at position $position (id: ${currentItem.id}, name: ${currentItem.name}) is selected: $isSelected")
    }

    // 선택 상태 토글 함수
    private fun toggleSelection(position: Int) {
        if (position >= itemCount) return

        val item = getItem(position)
        val wasSelected = selectedItems.contains(position)
        val wasInitiallySelected = initialSelectedItems.contains(position)
        val isImmutable = immutableItems.contains(position)

        Log.d("InviteListAdapter", "Toggle selection for position $position (id: ${item.id}, name: ${item.name}), was selected: $wasSelected, was initially selected: $wasInitiallySelected, immutable: $isImmutable")

        // ListDetailFragment에서 호출된 경우, 이미 선택된 항목은 선택 해제할 수 없음
        if (wasSelected && isImmutable) {
            Log.d("InviteListAdapter", "Item is immutable, ignoring deselection")
            return
        }

        // 토글 기능 구현
        if (wasSelected) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }

        // 즉시 아이템 상태 변경 알림
        notifyItemChanged(position)

        // 선택 상태 변경 확인 - 초기 상태와 다른지 비교
        // (1) 초기에 선택되었지만 지금은 선택되지 않은 경우 (선택 취소)
        // (2) 초기에 선택되지 않았지만 지금은 선택된 경우 (새로 선택)
        // 두 경우 모두 선택 상태가 변경된 것으로 처리
        checkSelectionChanged()

        Log.d("InviteListAdapter", "Selection changed: $selectionChanged, selectedItems: $selectedItems, initialItems: $initialSelectedItems, immutableItems: $immutableItems")

        // 선택 상태 변경 콜백 호출
        onSelectionChanged?.invoke(selectedItems.isNotEmpty(), selectionChanged)
    }

    // 선택 상태 변경 확인 메서드 (코드 중복 방지를 위해 별도 함수로 분리)
    private fun checkSelectionChanged() {
        // 각 항목별로 초기 상태와 현재 상태 비교
        val currentList = currentList
        if (currentList.isEmpty()) return

        // 초기 상태와 다른지 검사
        for (i in 0 until currentList.size) {
            val initiallySelected = initialSelectedItems.contains(i)
            val currentlySelected = selectedItems.contains(i)

            // 상태가 다르면 선택 상태가 변경된 것
            if (initiallySelected != currentlySelected) {
                selectionChanged = true
                return
            }
        }

        // 모든 항목이 초기 상태와 동일하면 변경 없음
        selectionChanged = false
    }

    // ViewHolder 클래스 - bind 메서드 없이 binding 객체만 노출
    class InviteViewHolder(val binding: ItemInviteListBinding) :
        RecyclerView.ViewHolder(binding.root)

    // 선택된 아이템들을 가져오는 메서드
    fun getSelectedItems(): List<InviteListItem> {
        return selectedItems.mapNotNull { position ->
            if (position < itemCount) getItem(position) else null
        }
    }

    // 현재 선택된 항목이 있는지 확인
    fun hasSelectedItems(): Boolean = selectedItems.isNotEmpty()

    // 선택 상태가 변경되었는지 확인
    fun hasSelectionChanged(): Boolean = selectionChanged

    // 사전 선택된 사용자 ID 목록 설정 메서드
    fun setPreSelectedIds(ids: List<Long>) {
        Log.d("InviteListAdapter", "Setting pre-selected IDs: $ids")
        this.preSelectedUserIds = ids
        initialSelectionApplied = false  // 새 사전 선택 ID 목록을 설정하면 초기화 플래그 재설정
        applyPreSelection()
    }

    // 사전 선택 적용 메서드 (별도 함수로 분리)
    private fun applyPreSelection() {
        if (initialSelectionApplied || preSelectedUserIds.isEmpty()) return

        val currentList = currentList
        if (currentList.isEmpty()) return

        // 기존 선택 초기화
        selectedItems.clear()
        initialSelectedItems.clear()
        immutableItems.clear() // 변경 불가능한 항목 초기화

        // 사전 선택된 ID에 해당하는 항목 선택
        currentList.forEachIndexed { index, item ->
            if (preSelectedUserIds.contains(item.id)) {
                selectedItems.add(index)
                initialSelectedItems.add(index)

                // ListDetailFragment에서 호출된 경우, 이 항목은 변경 불가능으로 표시
                if (isFromListDetail) {
                    immutableItems.add(index)
                }

                Log.d("InviteListAdapter", "Pre-selecting item at position $index (id: ${item.id}, name: ${item.name}), immutable: ${isFromListDetail}")
            }
        }

        // 선택 상태 변경 없음으로 설정
        selectionChanged = false

        // 변경된 항목만 UI 업데이트
        selectedItems.forEach { notifyItemChanged(it) }

        // 선택 상태 변경 알림
        onSelectionChanged?.invoke(selectedItems.isNotEmpty(), false)

        // 초기화 완료 플래그 설정
        initialSelectionApplied = true
    }

    // 현재 목록이 설정되면 사전 선택 상태 적용
    override fun submitList(list: List<InviteListItem>?) {
        super.submitList(list)

        // 리스트가 설정된 후 사전 선택 상태 적용
        list?.let {
            if (it.isNotEmpty() && preSelectedUserIds.isNotEmpty() && !initialSelectionApplied) {
                Log.d("InviteListAdapter", "Submitting list with ${it.size} items, applying pre-selections")
                applyPreSelection()
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<InviteListItem>() {
        override fun areItemsTheSame(oldItem: InviteListItem, newItem: InviteListItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: InviteListItem, newItem: InviteListItem): Boolean {
            return oldItem == newItem
        }
    }
}