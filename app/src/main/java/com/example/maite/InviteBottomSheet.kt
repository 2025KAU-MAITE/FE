package com.example.maite

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
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

        viewModel = ViewModelProvider(this)[InviteListViewModel::class.java]

        adapter = InviteListAdapter { hasSelection ->
            updateButtonState(hasSelection)
        }

        binding.inviteListRV.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@InviteBottomSheet.adapter
        }

        updateButtonState(false)

        viewModel.inviteList.observe(viewLifecycleOwner) { inviteList ->
            adapter.submitList(inviteList)
        }

        binding.cardView.setOnClickListener {
            viewModel.addInvite("새로운 초대")
            Toast.makeText(context, "검색 기능 구현 예정", Toast.LENGTH_SHORT).show()
        }

        binding.doneBtn.setOnClickListener {
            if (adapter.hasSelectedItems()) {
                val selectedItems = adapter.getSelectedItems()
                val selectedCount = selectedItems.size

                val resultBundle = bundleOf(KEY_SELECTED_COUNT to selectedCount)
                setFragmentResult(REQUEST_KEY, resultBundle)

                Toast.makeText(context, "${selectedItems.size}명 초대 완료", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(context, "초대할 사람을 선택해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateButtonState(hasSelection: Boolean) {
        if (hasSelection) {
            binding.btnBg.setColorFilter(Color.parseColor("#4C7EED"))
            binding.btnText.setTextColor(Color.parseColor("#FFFFFF"))
        } else {
            binding.btnBg.setColorFilter(Color.parseColor("#C4C4C4"))
            binding.btnText.setTextColor(Color.parseColor("#000000"))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "inviteResultRequestKey"
        const val KEY_SELECTED_COUNT = "selectedUserCount"

        fun newInstance(): InviteBottomSheet {
            return InviteBottomSheet()
        }
    }
}