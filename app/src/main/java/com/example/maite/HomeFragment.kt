package com.example.maite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.maite.databinding.FragmentHomeBinding
import com.example.maite.data.model.MeetingItem
import com.example.maite.data.model.MeetingProposal
import com.example.maite.ui.home.HomeViewModel

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class HomeFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 가장 가까운 회의 표시
        viewModel.nearestMeeting.observe(viewLifecycleOwner) { meeting: MeetingItem? ->
            if (meeting != null) {
                binding.tvMeetingLabel.visibility = View.VISIBLE
                binding.cardMeeting.visibility = View.VISIBLE
                binding.tvMeetingTitle.text = meeting.title
                binding.tvMeetingDate.text = "날짜: ${meeting.date}"
                binding.tvMeetingTime.text = "시간: ${meeting.startTime} ~ ${meeting.endTime}"
                binding.tvMeetingLocation.text = "장소: ${meeting.location}"
            } else {
                binding.tvMeetingLabel.visibility = View.GONE
                binding.cardMeeting.visibility = View.GONE
            }
        }

        // 읽지 않은 제안 표시
        viewModel.proposals.observe(viewLifecycleOwner) { proposals: List<MeetingProposal> ->
            if (proposals.isNotEmpty()) {
                val proposal = proposals.first()
                binding.tvProposalLabel.visibility = View.VISIBLE
                binding.cardProposal.visibility = View.VISIBLE
                binding.tvProposalTitle.text = proposal.title
                binding.tvProposalDate.text = "날짜: ${proposal.date}"
                binding.tvProposalTime.text = "시간: ${proposal.time}"
                binding.tvProposalLocation.text = "장소: ${proposal.location}"
            } else {
                binding.tvProposalLabel.visibility = View.GONE
                binding.cardProposal.visibility = View.GONE
            }
        }

        // 수락 버튼 클릭
        binding.btnAccept.setOnClickListener { _: View ->
            viewModel.proposals.value?.firstOrNull()?.let { proposal: MeetingProposal ->
                animateCardAndRemove(proposal, isAccepted = true)
            }
        }

        // 거절 버튼 클릭
        binding.btnDecline.setOnClickListener { _: View ->
            viewModel.proposals.value?.firstOrNull()?.let { proposal: MeetingProposal ->
                animateCardAndRemove(proposal, isAccepted = false)
            }
        }
    }

    private fun animateCardAndRemove(proposal: MeetingProposal, isAccepted: Boolean) {
        binding.cardProposal.animate()
            .alpha(0f)
            .translationY(100f) // ✅ 아래로 이동
            .setDuration(500)   // ✅ 좀 더 천천히
            .setInterpolator(AccelerateDecelerateInterpolator()) // ✅ 자연스러운 감속 애니메이션
            .withEndAction {
                if (isAccepted) {
                    viewModel.acceptProposal(proposal)
                } else {
                    viewModel.declineProposal(proposal)
                }

                // 뷰 초기화
                binding.cardProposal.visibility = View.GONE
                binding.tvProposalLabel.visibility = View.GONE
                binding.cardProposal.alpha = 1f
                binding.cardProposal.translationY = 0f
            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
