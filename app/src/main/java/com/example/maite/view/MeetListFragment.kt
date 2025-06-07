package com.example.maite

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.maite.databinding.FragmentMeetListBinding
import com.example.maite.model.MeetingDataManager
import com.example.maite.view.MeetListAdapter
import com.example.maite.viewmodel.MeetListViewModel
import kotlinx.coroutines.launch

class MeetListFragment : Fragment() {
    private var _binding: FragmentMeetListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MeetListViewModel by viewModels() // Factory 제거
    private lateinit var meetAdapter: MeetListAdapter
    private lateinit var meetingDataManager: MeetingDataManager

    private var fragmentRoomId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            fragmentRoomId = it.getLong(ARG_ROOM_ID, -1L)
            if (fragmentRoomId == -1L) fragmentRoomId = null
        }
        meetingDataManager = MeetingDataManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMeetListBinding.inflate(inflater, container, false)
        binding.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        parentFragmentManager.setFragmentResultListener("meeting_update_result", viewLifecycleOwner) { _, bundle ->
            val success = bundle.getBoolean("meeting_update_success", false)
            if (success) {
                Log.d("MeetListFragment", "회의 정보 업데이트됨, 목록 새로고침")
                loadDataAndUpdateViewModel()
            }
        }

        loadDataAndUpdateViewModel()

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        meetAdapter = MeetListAdapter { meetItem ->
            val meetingIdToPass: Long = meetItem.meetingId
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

        binding.meetListRV.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = meetAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.meetList.observe(viewLifecycleOwner) { list ->
            meetAdapter.submitList(list)
        }
    }

    private fun loadDataAndUpdateViewModel() {
        fragmentRoomId?.let { roomId ->
            viewLifecycleOwner.lifecycleScope.launch {
                Log.d("MeetListFragment", "roomId: $roomId 로 MeetingDataManager 호출하여 데이터 갱신 시도")
                val fetchSuccess = meetingDataManager.fetchAndDistributeMeetings(roomId)
                if (fetchSuccess) {
                    Log.d("MeetListFragment", "MeetingDataManager 데이터 갱신 성공, ViewModel 로드 호출")
                    viewModel.loadMeetList() // ViewModel의 loadMeetList() 호출
                } else {
                    Log.e("MeetListFragment", "MeetingDataManager 데이터 갱신 실패")
                    viewModel.loadMeetList()
                }
            }
        } ?: run {
            Log.w("MeetListFragment", "fragmentRoomId가 null입니다. 전체 지난 회의 목록을 로드합니다.")
            // fragmentRoomId가 null일 경우, MeetingDataManager를 roomId 없이 호출하는 로직이 없으므로
            // ViewModel이 현재 Repository의 전체 목록을 로드하도록 합니다.
            viewModel.loadMeetList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ROOM_ID = "room_id"

        fun newInstance(roomId: Long): MeetListFragment {
            return MeetListFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ROOM_ID, roomId)
                }
            }
        }
    }
}