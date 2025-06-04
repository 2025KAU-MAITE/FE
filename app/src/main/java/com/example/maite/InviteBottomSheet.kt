package com.example.maite

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.maite.databinding.BottomSheetInviteBinding
import com.example.maite.view.InviteListAdapter
import com.example.maite.viewmodel.InviteListViewModel
import com.example.maite.model.InviteListItem
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.ArrayList

class InviteBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetInviteBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: InviteListViewModel
    private lateinit var adapter: InviteListAdapter
    private lateinit var apiService: MaiteApiService

    // 이미 선택된 사용자 ID 목록
    private var preSelectedUserIds = ArrayList<Long>()

    // 호출 출처를 식별하는 플래그 추가
    private var isFromListDetail = false

    // 선택 상태가 변경되었는지 추적
    private var selectionChanged = false

    // 검색 관련 변수
    private var searchJob: Job? = null
    private var originalList = listOf<InviteListItem>()
    private var isSearchMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Bundle에서 사전 선택된 사용자 ID 목록 가져오기
        arguments?.let {
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
        apiService = MaiteRetrofitClient.getInstance(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[InviteListViewModel::class.java]

        adapter = InviteListAdapter(
            onSelectionChanged = { hasSelection, changed ->
                selectionChanged = changed
                updateButtonState(hasSelection, changed)
            },
            isFromListDetail = isFromListDetail,
            lifecycleOwner = viewLifecycleOwner // 라이프사이클 오너 전달 추가
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
            originalList = inviteList // 원본 목록 저장
            if (!isSearchMode) {
                adapter.submitList(inviteList)
                updateButtonState(adapter.hasSelectedItems(), adapter.hasSelectionChanged())
            }
        }

        // 에러 상태 관찰
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        }

        // 검색 기능 설정
        setupSearchFunctionality()

        binding.doneBtn.setOnClickListener {
            if (selectionChanged) {
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

    private fun setupSearchFunctionality() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchJob?.cancel()
                val query = s?.toString()?.trim() ?: ""

                if (query.isEmpty()) {
                    // 검색어가 비어있으면 원본 목록 표시
                    isSearchMode = false
                    adapter.submitList(originalList)
                } else {
                    // 검색어가 있으면 검색 수행 (디바운싱 적용)
                    searchJob = lifecycleScope.launch {
                        delay(300) // 300ms 디바운싱
                        performSearch(query)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // 키보드 검색 버튼 처리
        binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
                true
            } else {
                false
            }
        }
    }

    private fun performSearch(query: String) {
        lifecycleScope.launch {
            try {
                isSearchMode = true

                // API 검색만 수행 (로컬 친구 목록 필터링 제거)
                val response = apiService.searchUsers(query)

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val searchResults = response.body()?.result ?: emptyList()

                    // UserResult를 InviteListItem으로 변환
                    val apiResults = searchResults.map { userResult ->
                        InviteListItem(
                            id = userResult.id,
                            name = userResult.name,
                            email = userResult.email,
                            profileImageUrl = userResult.profileImageUrl
                        )
                    }.sortedBy { it.name }

                    adapter.submitList(apiResults)

                    if (apiResults.isEmpty()) {
                        Toast.makeText(context, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    adapter.submitList(emptyList())
                    Toast.makeText(context, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                adapter.submitList(emptyList())
                Toast.makeText(context, "검색 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 버튼 상태 업데이트 함수
    private fun updateButtonState(hasSelection: Boolean, selectionChanged: Boolean) {
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
        searchJob?.cancel()
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
        private const val ARG_FROM_LIST_DETAIL = "fromListDetail"

        fun newInstance(): InviteBottomSheet {
            return InviteBottomSheet()
        }

        fun newInstance(preSelectedUserIds: List<Long>, fromListDetail: Boolean = false): InviteBottomSheet {
            val fragment = InviteBottomSheet()
            val args = Bundle()

            val intList = ArrayList<Int>()
            preSelectedUserIds.forEach { id ->
                intList.add(id.toInt())
            }

            args.putIntegerArrayList(ARG_PRE_SELECTED_IDS, intList)
            args.putBoolean(ARG_FROM_LIST_DETAIL, fromListDetail)
            fragment.arguments = args
            return fragment
        }
    }
}