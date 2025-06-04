package com.example.maite

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.maite.databinding.FragmentMeetListBinding
import com.example.maite.view.MeetListAdapter
import com.example.maite.viewmodel.MeetListViewModel
import com.example.maite.model.MeetListRepository

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
        binding.root.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repoData = MeetListRepository.getInstance().getMeetList()

        setupRecyclerView()
        observeViewModel()

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}