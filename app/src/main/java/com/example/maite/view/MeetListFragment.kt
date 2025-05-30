package com.example.maite

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.maite.databinding.FragmentMeetListBinding
import com.example.maite.view.MeetListAdapter
import com.example.maite.viewmodel.MeetListViewModel
import com.example.maite.model.MeetListRepository
import android.util.Log

class MeetListFragment : Fragment() {
    private var _binding: FragmentMeetListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MeetListViewModel by viewModels()
    private lateinit var meetAdapter: MeetListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMeetListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 디버깅 목적: 저장소에서 직접 데이터 확인
        val repoData = MeetListRepository.getInstance().getMeetList()
        Log.d("MeetListFragment", "저장소 직접 접근 데이터: ${repoData.size}개 항목")

        setupRecyclerView()
        observeViewModel()

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        // 어댑터 초기화 (아이템 클릭 시 MeetDetailFragment로 전환)
        meetAdapter = MeetListAdapter { meetItem ->
            // MeetDetailFragment로 전환하는 로직
            val detailFragment = MeetDetailFragment.newInstance(meetItem)
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, detailFragment)
                .addToBackStack(null)
                .commit()

            Log.d("MeetListFragment", "회의 아이템 클릭됨: ${meetItem.title}, ${meetItem.date}")
        }

        binding.meetListRV.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = meetAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.meetList.observe(viewLifecycleOwner) { list ->
            Log.d("MeetListFragment", "회의 목록 업데이트됨: ${list.size}개 항목")
            meetAdapter.submitList(list)

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}