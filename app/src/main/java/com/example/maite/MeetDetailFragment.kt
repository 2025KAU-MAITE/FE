package com.example.maite

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat // Drawable/Color 가져오기 위해 필요
import androidx.navigation.fragment.findNavController // 뒤로가기 버튼용 (선택 사항)
import com.example.maite.databinding.FragmentMeetDetailBinding
import com.example.maite.model.MeetListItem // MeetListItem 모델 import

private const val ARG_MEET_ITEM = "meet_item"

const val ARG_CURRENT_TITLE = "current_title"
const val ARG_CURRENT_DATE = "current_date"
const val ARG_CURRENT_TIME = "current_time"
const val ARG_CURRENT_PLACE = "current_place"

class MeetDetailFragment : Fragment() {
    private var _binding: FragmentMeetDetailBinding? = null
    private val binding get() = _binding!!

    private var meetItem: MeetListItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            meetItem = it.getParcelable(ARG_MEET_ITEM)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMeetDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        meetItem?.let {
            binding.meetTitle.text = it.title
            binding.meetDate.text = it.date
            binding.meetTime.text = it.time
            binding.meetPlace.text = it.place
        }

        setupTabs()
        selectTab(isMinutesTabSelected = true)


        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.editBtn.setOnClickListener {
            val editMeetBottomSheet = EditMeetBottomSheet()

            val args = Bundle().apply {
                putString(ARG_CURRENT_TITLE, binding.meetTitle.text.toString())
                putString(ARG_CURRENT_DATE, binding.meetDate.text.toString())
                putString(ARG_CURRENT_TIME, binding.meetTime.text.toString())
                putString(ARG_CURRENT_PLACE, binding.meetPlace.text.toString())
            }
            editMeetBottomSheet.arguments = args

            editMeetBottomSheet.show(childFragmentManager, EditMeetBottomSheet.TAG)
        }

        binding.tabMinutes.setOnClickListener {
            selectTab(isMinutesTabSelected = true)
        }

        binding.tabSummary.setOnClickListener {
            selectTab(isMinutesTabSelected = false)
        }

        binding.recordBtn.setOnClickListener {
            // TODO: 녹음하기 버튼 클릭 처리
        }
        binding.uploadBtn.setOnClickListener {
            // TODO: 회의록 첨부하기 버튼 클릭 처리
        }
        binding.summurizeBtn.setOnClickListener {
            // TODO: 요약본 첨부하기 버튼 클릭 처리
        }
    }

    private fun setupTabs() {
        binding.tabMinutes.isSelected = false
        binding.tabSummary.isSelected = false
        binding.tabMinutes.setBackgroundResource(R.drawable.tab_unselected_background)
        binding.tabSummary.setBackgroundResource(R.drawable.tab_unselected_background)
        binding.minutesContentContainer.visibility = View.GONE
        binding.summaryContentContainer.visibility = View.GONE
    }

    private fun selectTab(isMinutesTabSelected: Boolean) {
        if (isMinutesTabSelected) {
            binding.tabMinutes.isSelected = true
            binding.tabSummary.isSelected = false
            binding.tabMinutes.setBackgroundResource(R.drawable.tab_selected_background)
            binding.tabSummary.setBackgroundResource(R.drawable.tab_unselected_background)

            binding.minutesContentContainer.visibility = View.VISIBLE
            binding.summaryContentContainer.visibility = View.GONE

        } else {
            binding.tabMinutes.isSelected = false
            binding.tabSummary.isSelected = true
            binding.tabMinutes.setBackgroundResource(R.drawable.tab_unselected_background)
            binding.tabSummary.setBackgroundResource(R.drawable.tab_selected_background)
            
            binding.minutesContentContainer.visibility = View.GONE
            binding.summaryContentContainer.visibility = View.VISIBLE
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(meetItem: MeetListItem) =
            MeetDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_MEET_ITEM, meetItem)
                }
            }
    }
}