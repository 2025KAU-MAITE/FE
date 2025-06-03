package com.example.maite.view

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.example.maite.MaiteRetrofitClient
import com.example.maite.R
import com.example.maite.UserResult
import com.example.maite.databinding.ItemInviteListBinding
import com.example.maite.model.InviteListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InviteListAdapter(
    private val onSelectionChanged: ((Boolean, Boolean) -> Unit)? = null,
    private val isFromListDetail: Boolean = false,
    private val lifecycleOwner: LifecycleOwner // 추가된 파라미터
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
        return InviteViewHolder(binding, lifecycleOwner)
    }

    override fun onBindViewHolder(holder: InviteViewHolder, position: Int) {
        val currentItem = getItem(position)

        // 현재 선택 상태에 따라 UI 업데이트
        val isSelected = selectedItems.contains(position)

        // bind 메서드 호출 (프로필 이미지 로딩 포함)
        holder.bind(currentItem, isSelected) { toggleSelection(position) }

        Log.d("InviteListAdapter", "Item at position $position (id: ${currentItem.id}, name: ${currentItem.name}) is selected: $isSelected")
    }

    // 선택 상태 토글 함수 (기존 로직 유지)
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
        checkSelectionChanged()

        Log.d("InviteListAdapter", "Selection changed: $selectionChanged, selectedItems: $selectedItems, initialItems: $initialSelectedItems, immutableItems: $immutableItems")

        // 선택 상태 변경 콜백 호출
        onSelectionChanged?.invoke(selectedItems.isNotEmpty(), selectionChanged)
    }

    // 선택 상태 변경 확인 메서드 (기존 로직 유지)
    private fun checkSelectionChanged() {
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

    // ViewHolder 클래스 - 프로필 이미지 로딩 기능 추가
    class InviteViewHolder(
        val binding: ItemInviteListBinding,
        private val lifecycleOwner: LifecycleOwner
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: InviteListItem,
            isSelected: Boolean,
            onItemClick: () -> Unit
        ) {
            // 기본 정보 설정
            binding.name.text = item.name

            // 이메일 표시 (이메일이 있으면 표시, 없으면 숨김)
            if (!item.email.isNullOrBlank()) {
                binding.email.text = item.email
                binding.email.visibility = View.VISIBLE
            } else {
                binding.email.visibility = View.GONE
            }

            // 선택 상태를 CardView의 isSelected로 설정 (기존 테두리 색상 변경 방식 유지)
            binding.inviteCardView.isSelected = isSelected

            // 클릭 리스너 설정
            binding.root.setOnClickListener { onItemClick() }
            binding.inviteCardView.setOnClickListener { onItemClick() }

            // 프로필 이미지 로드
            loadProfileImage(item)
        }

        private fun loadProfileImage(item: InviteListItem) {
            // 이미 URL이 있는 경우 바로 로드
            if (!item.profileImageUrl.isNullOrBlank()) {
                Glide.with(binding.root.context)
                    .load(item.profileImageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .skipMemoryCache(true)
                    .signature(ObjectKey(System.currentTimeMillis().toString()))
                    .placeholder(R.drawable.img_profile_default)
                    .error(R.drawable.img_profile_default)
                    .into(binding.profileImg)
            } else if (!item.email.isNullOrBlank()) {
                // URL이 없고 이메일이 있는 경우 API로 검색
                loadProfileImageFromApi(item.email!!)
            } else {
                // URL도 이메일도 없는 경우 기본 이미지
                binding.profileImg.setImageResource(R.drawable.img_profile_default)
            }
        }

        private fun loadProfileImageFromApi(email: String) {
            lifecycleOwner.lifecycleScope.launch {
                try {
                    val apiService = MaiteRetrofitClient.getInstance(binding.root.context)
                    val response = withContext(Dispatchers.IO) {
                        apiService.searchUsers(email)
                    }

                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        val userList: List<UserResult>? = response.body()?.result
                        val userInfo: UserResult? = userList?.firstOrNull { it.email == email }
                            ?: userList?.firstOrNull()

                        val profileImageUrl = userInfo?.profileImageUrl

                        if (!profileImageUrl.isNullOrBlank()) {
                            Glide.with(binding.root.context)
                                .load(profileImageUrl)
                                .apply(RequestOptions.circleCropTransform())
                                .skipMemoryCache(true)
                                .signature(ObjectKey(System.currentTimeMillis().toString()))
                                .placeholder(R.drawable.img_profile_default)
                                .error(R.drawable.img_profile_default)
                                .into(binding.profileImg)
                        } else {
                            binding.profileImg.setImageResource(R.drawable.img_profile_default)
                        }
                    } else {
                        binding.profileImg.setImageResource(R.drawable.img_profile_default)
                        Log.e("InviteListAdapter", "Failed to fetch profile for $email: ${response.code()}")
                    }
                } catch (e: Exception) {
                    binding.profileImg.setImageResource(R.drawable.img_profile_default)
                    Log.e("InviteListAdapter", "Exception loading profile for $email", e)
                }
            }
        }
    }

    // 기존 메서드들 유지
    fun getSelectedItems(): List<InviteListItem> {
        return selectedItems.mapNotNull { position ->
            if (position < itemCount) getItem(position) else null
        }
    }

    fun hasSelectedItems(): Boolean = selectedItems.isNotEmpty()

    fun hasSelectionChanged(): Boolean = selectionChanged

    fun setPreSelectedIds(ids: List<Long>) {
        Log.d("InviteListAdapter", "Setting pre-selected IDs: $ids")
        this.preSelectedUserIds = ids
        initialSelectionApplied = false
        applyPreSelection()
    }

    private fun applyPreSelection() {
        if (initialSelectionApplied || preSelectedUserIds.isEmpty()) return

        val currentList = currentList
        if (currentList.isEmpty()) return

        // 기존 선택 초기화
        selectedItems.clear()
        initialSelectedItems.clear()
        immutableItems.clear()

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

    override fun submitList(list: List<InviteListItem>?) {
        super.submitList(list)

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