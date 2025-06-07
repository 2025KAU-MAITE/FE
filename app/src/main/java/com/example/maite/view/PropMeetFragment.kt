package com.example.maite

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope // lifecycleScope import 추가
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.maite.databinding.FragmentPropMeetBinding
import com.example.maite.model.MeetingDataManager // MeetingDataManager import 추가
import com.example.maite.model.PropMeetItem
import com.example.maite.view.PropMeetAdapter
import com.example.maite.view.PropMeetClickListener
import com.example.maite.viewmodel.PropMeetViewModel
import kotlinx.coroutines.launch // launch import 추가

class PropMeetFragment : Fragment(), PropMeetClickListener {
    private var _binding: FragmentPropMeetBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PropMeetViewModel
    private lateinit var adapter: PropMeetAdapter
    private lateinit var meetingDataManager: MeetingDataManager // MeetingDataManager 인스턴스 추가

    private var fragmentRoomId: Long? = null // roomId를 저장할 변수

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            fragmentRoomId = it.getLong(ARG_ROOM_ID, -1L)
            if (fragmentRoomId == -1L) fragmentRoomId = null
        }
        meetingDataManager = MeetingDataManager(requireContext()) // MeetingDataManager 초기화
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPropMeetBinding.inflate(inflater, container, false)
        binding.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            PropMeetViewModel.Factory(requireActivity().application)
        )[PropMeetViewModel::class.java]

        adapter = PropMeetAdapter(this) { clickedItem ->
            val meetingIdToPass: Long = clickedItem.meetingId

            val detailFragment = MeetDetailFragment.newInstance(meetingIdToPass)
            val fragmentTag = MeetDetailFragment::class.java.name

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    0,
                    0,
                    R.anim.slide_out_right
                )
                .add(R.id.main_frm, detailFragment, fragmentTag)
                .addToBackStack(fragmentTag)
                .commit()
        }

        binding.propRV.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PropMeetFragment.adapter
        }

        viewModel.proposedMeetings.observe(viewLifecycleOwner) { meetings ->
            adapter.submitList(meetings)
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                // 오류 처리
            }
        }

        // MeetDetailFragment에서 회의 정보가 업데이트되었을 때 결과를 수신합니다.
        parentFragmentManager.setFragmentResultListener("meeting_update_result", viewLifecycleOwner) { _, bundle ->
            val success = bundle.getBoolean("meeting_update_success", false)
            if (success) {
                Log.d("PropMeetFragment", "회의 정보 업데이트됨, 목록 새로고침")
                loadDataAndUpdateViewModel() // 데이터 로드 및 ViewModel 업데이트 함수 호출
            }
        }

        loadDataAndUpdateViewModel() // 초기 데이터 로드

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadDataAndUpdateViewModel() {
        fragmentRoomId?.let { roomId ->
            viewLifecycleOwner.lifecycleScope.launch {
                Log.d("PropMeetFragment", "roomId: $roomId 로 MeetingDataManager 호출하여 데이터 갱신 시도")
                val fetchSuccess = meetingDataManager.fetchAndDistributeMeetings(roomId)
                if (fetchSuccess) {
                    Log.d("PropMeetFragment", "MeetingDataManager 데이터 갱신 성공, ViewModel 로드 호출")
                    viewModel.loadProposedMeetings() // Repository가 업데이트되었으므로 ViewModel이 이를 로드
                } else {
                    Log.e("PropMeetFragment", "MeetingDataManager 데이터 갱신 실패")
                    // 필요시 오류 처리 또는 기존 데이터로 ViewModel 로드
                    viewModel.loadProposedMeetings()
                }
            }
        } ?: run {
            Log.w("PropMeetFragment", "fragmentRoomId가 null이므로 전체 목록 로드 시도 (또는 오류 처리)")
            // roomId가 없는 경우의 동작 정의 (예: 모든 제안된 미팅 로드 또는 오류 메시지)
            // 현재는 viewModel.loadProposedMeetings()를 호출하여 Repository의 현재 상태를 로드합니다.
            // 이 경우, Repository는 다른 곳에서 채워졌어야 합니다.
            viewModel.loadProposedMeetings()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onAcceptClick(item: PropMeetItem) {
        if (item.acceptance != "PENDING") {
            return
        }
        viewModel.acceptMeeting(item)
    }

    override fun onRejectClick(item: PropMeetItem) {
        if (item.acceptance != "PENDING") {
            return
        }
        viewModel.rejectMeeting(item)
    }

    companion object {
        private const val ARG_ROOM_ID = "room_id" // roomId 전달을 위한 키

        fun newInstance(roomId: Long): PropMeetFragment {
            return PropMeetFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ROOM_ID, roomId)
                }
            }
        }
    }
}