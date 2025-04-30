package com.example.maite

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.maite.databinding.BottomSheetInviteBinding
import com.example.maite.model.InviteListItem
import com.example.maite.view.InviteListAdapter
import com.example.maite.viewmodel.InviteListViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class InviteBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetInviteBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: InviteListViewModel
    private lateinit var adapter: InviteListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetInviteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[InviteListViewModel::class.java]

        // Initialize adapter with selection callback
        adapter = InviteListAdapter { hasSelection ->
            // 선택 상태에 따라 버튼 색상 업데이트
            updateButtonState(hasSelection)
        }

        // Set up RecyclerView
        binding.inviteListRV.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@InviteBottomSheet.adapter
        }

        // 초기 버튼 상태 설정 (아무것도 선택되지 않은 상태)
        updateButtonState(false)

        // Observe changes to the invite list
        viewModel.inviteList.observe(viewLifecycleOwner) { inviteList ->
            adapter.submitList(inviteList)
        }

        // Set up search functionality (placeholder for now)
        binding.cardView.setOnClickListener {
            // Here you would typically show a search dialog or implement search functionality
            // For demonstration, just add a new invite directly
            viewModel.addInvite("새로운 초대")
            Toast.makeText(context, "검색 기능 구현 예정", Toast.LENGTH_SHORT).show()
        }

        // Set up completion button
        binding.doneBtn.setOnClickListener {
            if (adapter.hasSelectedItems()) {
                // 선택된 항목이 있을 때의 처리
                val selectedItems = adapter.getSelectedItems()
                // 선택된 항목으로 작업 수행...
                Toast.makeText(context, "${selectedItems.size}명 초대 완료", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                // 선택된 항목이 없을 때
                Toast.makeText(context, "초대할 사람을 선택해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 버튼 상태 업데이트 함수
    private fun updateButtonState(hasSelection: Boolean) {
        if (hasSelection) {
            // 적어도 하나의 항목이 선택된 경우
            binding.btnBg.setColorFilter(Color.parseColor("#4C7EED"))
            binding.btnText.setTextColor(Color.parseColor("#FFFFFF"))
        } else {
            // 아무것도 선택되지 않은 경우
            binding.btnBg.setColorFilter(Color.parseColor("#C4C4C4"))
            binding.btnText.setTextColor(Color.parseColor("#000000"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): InviteBottomSheet {
            return InviteBottomSheet()
        }
    }
}