package com.example.maite

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.maite.databinding.FragmentPropMeetBinding
// import com.example.maite.model.MeetingDataManager // ViewModel을 사용하므로 주석 처리 또는 제거
import com.example.maite.model.PropMeetItem
import com.example.maite.view.PropMeetAdapter
import com.example.maite.view.PropMeetClickListener
import com.example.maite.viewmodel.PropMeetViewModel

class PropMeetFragment : Fragment(), PropMeetClickListener {
    private var _binding: FragmentPropMeetBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PropMeetViewModel
    private lateinit var adapter: PropMeetAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPropMeetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(
            this,
            PropMeetViewModel.Factory(requireActivity().application)
        )[PropMeetViewModel::class.java]

        // 어댑터 초기화 시 아이템 클릭 리스너 추가
        adapter = PropMeetAdapter(this) { clickedItem ->
            // PropMeetItem의 meetingId 필드를 사용합니다.
            val meetingIdToPass: Long = clickedItem.meetingId

            val detailFragment = MeetDetailFragment.newInstance(meetingIdToPass)
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, detailFragment) // R.id.main_frm은 실제 프래그먼트 컨테이너 ID여야 합니다.
                .addToBackStack(null)
                .commit()

            Log.d("PropMeetFragment", "제안된 회의 아이템 클릭됨: ${clickedItem.title}, MeetingID: $meetingIdToPass, MeetDetailFragment로 이동")
        }

        binding.propRV.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PropMeetFragment.adapter
        }

        viewModel.proposedMeetings.observe(viewLifecycleOwner) { meetings ->
            Log.d("PropMeetFragment", "ViewModel data count: ${meetings.size}")
            adapter.submitList(meetings)
            meetings.forEach { item ->
                Log.d("PropMeetFragment", "ViewModel Item: ${item.title}, date: ${item.date}, status: ${item.acceptance}, meetingId: ${item.meetingId}") // meetingId 로깅
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        viewModel.loadProposedMeetings()

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onAcceptClick(item: PropMeetItem) {
        if (item.acceptance != "PENDING") {
            Toast.makeText(requireContext(), "이미 처리된 회의입니다", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.acceptMeeting(item)
        Log.d("PropMeetFragment", "수락 클릭: ${item.title}, meetingId: ${item.meetingId}") // meetingId 로깅
    }

    override fun onRejectClick(item: PropMeetItem) {
        if (item.acceptance != "PENDING") {
            Toast.makeText(requireContext(), "이미 처리된 회의입니다", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.rejectMeeting(item)
        Log.d("PropMeetFragment", "거절 클릭: ${item.title}, meetingId: ${item.meetingId}") // meetingId 로깅
    }
}