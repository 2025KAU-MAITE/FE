package com.example.maite

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
            meetings.forEach { item ->
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
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
}