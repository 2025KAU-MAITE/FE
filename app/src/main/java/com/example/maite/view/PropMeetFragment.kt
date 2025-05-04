package com.example.maite

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast // Toast import 추가
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.maite.databinding.FragmentPropMeetBinding
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

        viewModel = ViewModelProvider(this)[PropMeetViewModel::class.java]
        adapter = PropMeetAdapter(this)

        binding.propRV.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PropMeetFragment.adapter
        }

        viewModel.proposedMeetings.observe(viewLifecycleOwner) { meetings ->
            // LiveData 변경 시 어댑터에 새 리스트 제출 (변경된 리스트가 반영됨)
            adapter.submitList(meetings)
        }

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