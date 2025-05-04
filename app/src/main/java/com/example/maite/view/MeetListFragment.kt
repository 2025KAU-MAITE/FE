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
// import android.widget.Toast // Toast 제거 또는 주석 처리

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

        setupRecyclerView()
        observeViewModel()

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        // 어댑터 초기화 (아이템 클릭 시 MeetDetailFragment로 전환)
        meetAdapter = MeetListAdapter { meetItem ->
            // *** START: 아이템 클릭 시 MeetDetailFragment로 전환하는 로직 ***
            val detailFragment = MeetDetailFragment.newInstance(meetItem)
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, detailFragment) // R.id.main_frm은 실제 컨테이너 ID로 변경
                .addToBackStack(null) // 백스택에 추가하여 뒤로가기 가능하게 함
                .commit()
            // *** END: 아이템 클릭 시 MeetDetailFragment로 전환하는 로직 ***
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}