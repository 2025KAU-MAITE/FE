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
import com.example.maite.model.MeetingDataManager
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

        // ViewModel 초기화
        viewModel = ViewModelProvider(
            this,
            PropMeetViewModel.Factory(requireActivity().application)
        )[PropMeetViewModel::class.java]

        // 로깅 추가: MeetingDataManager에서 직접 데이터 확인
        val meetingDataManager = MeetingDataManager(requireContext())
        val propMeetings = meetingDataManager.getPropMeetRepository().getProposedMeetings()
        Log.d("PropMeetFragment", "Repository data count: ${propMeetings.size}")
        propMeetings.forEach { item ->
            Log.d("PropMeetFragment", "Item: ${item.title}, date: ${item.date}, status: ${item.acceptance}")
        }

        // 어댑터 초기화
        adapter = PropMeetAdapter(this)

        // RecyclerView 설정
        binding.propRV.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PropMeetFragment.adapter
        }

        // 제안된 회의 데이터 관찰
        viewModel.proposedMeetings.observe(viewLifecycleOwner) { meetings ->
            Log.d("PropMeetFragment", "ViewModel data count: ${meetings.size}")
            adapter.submitList(meetings)

            // 로그 추가: 실제로 어댑터에 전달된 데이터 확인
            meetings.forEach { item ->
                Log.d("PropMeetFragment", "ViewModel Item: ${item.title}, date: ${item.date}")
            }
        }

        // 수동으로 데이터 로드 요청 (문제 해결을 위해 추가)
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
        viewModel.acceptMeeting(item) // ViewModel에 수락 처리 및 제거 요청
        // Toast 메시지 표시
        Toast.makeText(requireContext(), "회의를 수락했습니다", Toast.LENGTH_SHORT).show()
    }

    override fun onRejectClick(item: PropMeetItem) {
        viewModel.rejectMeeting(item) // ViewModel에 거절 처리 및 제거 요청
        // Toast 메시지 표시
        Toast.makeText(requireContext(), "회의를 거절했습니다", Toast.LENGTH_SHORT).show()
    }
}