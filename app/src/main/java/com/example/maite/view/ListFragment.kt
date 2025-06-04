package com.example.maite.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.maite.CreateMaiteBottomSheet
import com.example.maite.ListDetailFragment
import com.example.maite.R
import com.example.maite.databinding.FragmentListBinding
import com.example.maite.viewmodel.MaiteListViewModel

class ListFragment : Fragment() {
    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MaiteListViewModel
    private lateinit var adapter: MaiteListAdapter

    // 중복 탐색을 방지하기 위한 플래그 추가
    private var isNavigating = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel 초기화
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application))
            .get(MaiteListViewModel::class.java)

        // 어댑터 초기화 및 클릭 리스너 설정
        setupRecyclerView()

        // ViewModel에서 LiveData 관찰
        observeViewModel()

        // 방 상세 정보 관찰 추가
        observeRoomDetail()

        binding.doneBtn.setOnClickListener {
            val bottomSheet = CreateMaiteBottomSheet.newInstance()
            bottomSheet.show(childFragmentManager, CreateMaiteBottomSheet.TAG)
        }

        // 방 생성 완료 이벤트 리스너 설정
        setupFragmentResultListener()

        // 회의방 초대에서 수락한 경우 해당 회의방으로 이동
        checkLastJoinedRoom()
    }

    override fun onResume() {
        super.onResume()
        // 프래그먼트가 다시 표시될 때 탐색 플래그 리셋
        isNavigating = false
    }

    // 방 상세 정보 LiveData 관찰 메서드
    private fun observeRoomDetail() {
        viewModel.roomDetail.observe(viewLifecycleOwner) { roomItem ->
            // 이미 탐색 중이라면 무시
            if (isNavigating) return@observe

            // RoomItem을 MaiteListItem으로 변환
            val maiteListItem = viewModel.convertRoomItemToMaiteListItem(roomItem)

            // ListDetailFragment로 전환
            navigateToDetailFragment(maiteListItem)
        }
    }

    // ListDetailFragment로 이동하는 메서드
    private fun navigateToDetailFragment(maiteListItem: com.example.maite.model.MaiteListItem) {
        // 중복 탐색 방지
        if (isNavigating) return
        isNavigating = true

        val fragment = ListDetailFragment.newInstance(maiteListItem)
        // 명시적으로 트랜잭션을 제대로 설정
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                0,
                R.anim.slide_out_right
            )
            .replace(R.id.main_frm, fragment)
            .addToBackStack(null) // 뒤로가기를 위한 백스택 추가
            .commit()
    }

    private fun setupFragmentResultListener() {
        // CreateMaiteBottomSheet에서 방 생성 완료 이벤트 수신
        childFragmentManager.setFragmentResultListener(
            CreateMaiteBottomSheet.ROOM_CREATED_REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val isRoomCreated = bundle.getBoolean(CreateMaiteBottomSheet.ROOM_CREATED_RESULT_KEY, false)
            if (isRoomCreated) {
                // 방 생성이 완료되면 목록 새로고침
                refreshMaiteList()
            }
        }
    }

    // 목록 새로고침 함수
    private fun refreshMaiteList() {
        // ViewModel을 통해 데이터 새로고침
        viewModel.loadMaiteList()
    }

    private fun setupRecyclerView() {
        adapter = MaiteListAdapter { maiteListItem ->
            // 중복 탐색 방지
            if (isNavigating) return@MaiteListAdapter

            // 수정된 부분: roomId를 사용하여 API 호출
            val roomId = maiteListItem.roomId
            if (roomId != null) {
                // 방 상세 정보 API 호출
                viewModel.getRoomDetail(roomId)
            } else {
                Toast.makeText(requireContext(), "방 ID가 없습니다", Toast.LENGTH_SHORT).show()

                // roomId가 없는 경우 직접 디테일 페이지로 이동 (API 호출 없이)
                navigateToDetailFragment(maiteListItem)
            }
        }

        binding.listRV.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ListFragment.adapter
        }
    }

    private fun observeViewModel() {
        // Maite 목록 관찰
        viewModel.maiteList.observe(viewLifecycleOwner) { maiteList ->
            adapter.submitList(maiteList)
        }

        // 오류 메시지 관찰
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.listRV.adapter = null // 메모리 누수 방지를 위해 어댑터 참조 해제
        _binding = null
    }

    private fun checkLastJoinedRoom() {
        val preferencesUtil = com.example.maite.PreferencesUtil(requireContext())
        val roomId = preferencesUtil.getLastJoinedRoomId()

        if (roomId != null) {
            android.util.Log.d("ListFragment", "마지막으로 참가한 회의방 ID: $roomId")

            // 이미 저장된 방 ID가 있으면 해당 방으로 이동
            viewModel.getRoomDetail(roomId.toLong())

            // 처리 완료 후 저장된 방 ID 초기화
            preferencesUtil.clearLastJoinedRoomId()
        } else {
            android.util.Log.d("ListFragment", "마지막으로 참가한 회의방 ID 없음")

            // 초기 데이터 로드
            refreshMaiteList()
        }
    }
}