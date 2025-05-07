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
import com.example.maite.view.InviteListAdapter
import com.example.maite.viewmodel.InviteListViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.ArrayList

class InviteBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetInviteBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: InviteListViewModel
    private lateinit var adapter: InviteListAdapter

    // 이미 선택된 사용자 ID 목록
    private var preSelectedUserIds = ArrayList<Long>()

    // 호출 출처를 식별하는 플래그 추가
    private var isFromListDetail = false

    // 선택 상태가 변경되었는지 추적
    private var selectionChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bundle에서 사전 선택된 사용자 ID 목록 가져오기
        arguments?.let {
            // ArrayList로 가져오기 (long[] 대신)
            @Suppress("UNCHECKED_CAST")
            val idsList = it.getIntegerArrayList(ARG_PRE_SELECTED_IDS)?.map { id -> id.toLong() }
            if (idsList != null) {
                preSelectedUserIds.addAll(idsList)
            }

            // 호출 출처 플래그 가져오기
            isFromListDetail = it.getBoolean(ARG_FROM_LIST_DETAIL, false)
        }
    }

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

        adapter = InviteListAdapter(
            onSelectionChanged = { hasSelection, changed ->
                // 선택 상태가 변경되었을 때만 버튼 활성화 (항목 선택 여부와 관계없이)
                selectionChanged = changed
                updateButtonState(hasSelection, changed)
            },
            isFromListDetail = isFromListDetail
        )

        // 사전 선택된 사용자 ID 목록 설정
        if (preSelectedUserIds.isNotEmpty()) {
            adapter.setPreSelectedIds(preSelectedUserIds)
        }

        binding.inviteListRV.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@InviteBottomSheet.adapter
        }

        // 초기 상태에서는 버튼 비활성화
        updateButtonState(false, false)

        // 초대 목록 관찰
        viewModel.inviteList.observe(viewLifecycleOwner) { inviteList ->
            adapter.submitList(inviteList)
            // 목록이 로드된 후 선택 상태 업데이트
            updateButtonState(adapter.hasSelectedItems(), adapter.hasSelectionChanged())
        }

        // 에러 상태 관찰
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        }

        binding.cardView.setOnClickListener {
            Toast.makeText(context, "검색 기능 구현 예정", Toast.LENGTH_SHORT).show()
        }

        binding.doneBtn.setOnClickListener {
            if (selectionChanged) {  // 선택 항목 여부와 관계없이 변경되었으면 완료 가능
                val selectedItems = adapter.getSelectedItems()
                val selectedCount = selectedItems.size

                // 선택된 사용자의 ID, 이름, 프로필 URL, 이메일을 리스트로 수집
                val selectedIds = ArrayList<Long>()
                val selectedNames = ArrayList<String>()
                val selectedProfileUrls = ArrayList<String>()
                val selectedEmails = ArrayList<String>()

                for (item in selectedItems) {
                    selectedIds.add(item.id)
                    selectedNames.add(item.name)
                    selectedProfileUrls.add(item.profileImageUrl ?: "")
                    selectedEmails.add(item.email ?: "")
                }

                // 결과 Bundle에 추가 정보 담기
                val resultBundle = bundleOf(
                    KEY_SELECTED_COUNT to selectedCount,
                    KEY_SELECTED_IDS to selectedIds,
                    KEY_SELECTED_NAMES to selectedNames,
                    KEY_SELECTED_PROFILE_URLS to selectedProfileUrls,
                    KEY_SELECTED_EMAILS to selectedEmails
                )

                setFragmentResult(REQUEST_KEY, resultBundle)

                dismiss()
            } else {
                Toast.makeText(context, "선택 상태를 변경해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 버튼 상태 업데이트 함수
    private fun updateButtonState(hasSelection: Boolean, selectionChanged: Boolean) {
        // 선택 상태가 변경된 경우에만 버튼 활성화
        // (선택항목이 없어도 이전 선택항목을 모두 취소한 것이므로 활성화)
        if (selectionChanged) {
            binding.btnBg.setColorFilter(Color.parseColor("#4C7EED"))
            binding.btnText.setTextColor(Color.parseColor("#FFFFFF"))
            binding.doneBtn.isClickable = true
        } else {
            binding.btnBg.setColorFilter(Color.parseColor("#C4C4C4"))
            binding.btnText.setTextColor(Color.parseColor("#000000"))
            binding.doneBtn.isClickable = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "inviteResultRequestKey"
        const val KEY_SELECTED_COUNT = "selectedUserCount"
        const val KEY_SELECTED_IDS = "selectedUserIds"
        const val KEY_SELECTED_NAMES = "selectedUserNames"
        const val KEY_SELECTED_PROFILE_URLS = "selectedUserProfileUrls"
        const val KEY_SELECTED_EMAILS = "selectedUserEmails"

        private const val ARG_PRE_SELECTED_IDS = "preSelectedUserIds"
        private const val ARG_FROM_LIST_DETAIL = "fromListDetail" // 새로운 상수 추가

        fun newInstance(): InviteBottomSheet {
            return InviteBottomSheet()
        }

        // 사전 선택된 사용자 ID 목록을 받는 newInstance 메서드 추가
        fun newInstance(preSelectedUserIds: List<Long>, fromListDetail: Boolean = false): InviteBottomSheet {
            val fragment = InviteBottomSheet()
            val args = Bundle()

            // Int ArrayList로 변환하여 전달 (문제 해결을 위해)
            val intList = ArrayList<Int>()
            preSelectedUserIds.forEach { id ->
                // Long을 Int로 변환 (ID가 Int 범위를 초과하지 않는다고 가정)
                intList.add(id.toInt())
            }

            args.putIntegerArrayList(ARG_PRE_SELECTED_IDS, intList)
            args.putBoolean(ARG_FROM_LIST_DETAIL, fromListDetail) // 추가된 인자
            fragment.arguments = args
            return fragment
        }
    }
}