package com.example.maite

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
// import androidx.fragment.app.setFragmentResultListener // 이 import는 제거해도 됩니다.
import com.example.maite.databinding.FragmentMeetDetailBinding
import com.example.maite.model.MeetListItem

private const val ARG_MEET_ITEM = "meet_item"

const val ARG_CURRENT_TITLE = "current_title"
const val ARG_CURRENT_DATE = "current_date"
const val ARG_CURRENT_TIME = "current_time"
const val ARG_CURRENT_PLACE = "current_place"

class MeetDetailFragment : Fragment() {
    private var _binding: FragmentMeetDetailBinding? = null
    private val binding get() = _binding

    private var meetItem: MeetListItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            meetItem = it.getParcelable(ARG_MEET_ITEM)
        }

        // Fragment Result Listener 설정 - childFragmentManager 사용!
        childFragmentManager.setFragmentResultListener(UploadBottomSheet.REQUEST_KEY_UPLOAD, this) { requestKey, bundle -> // lifecycleOwner를 this로 전달
            Log.d(TAG, "Fragment Result Received! Request Key: $requestKey")

            val success = bundle.getBoolean(UploadBottomSheet.BUNDLE_KEY_SUCCESS)
            if (success) {
                Log.d(TAG, "Upload 성공 결과 수신")
                val responseBody = bundle.getString(UploadBottomSheet.BUNDLE_KEY_RESPONSE)
                // UI 업데이트는 메인 스레드에서 실행되도록 보장
                requireActivity().runOnUiThread {
                    binding?.let { // Null-safe call
                        showSummaryView(responseBody ?: "요약본이 생성되었습니다.")
                    } ?: Log.e(TAG, "결과 수신 시 binding이 null입니다.")
                }
            } else {
                Log.d(TAG, "Upload 실패 결과 수신 (또는 결과 없음)")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMeetDetailBinding.inflate(inflater, container, false)
        return _binding!!.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.let { b ->
            meetItem?.let {
                b.meetTitle.text = it.title
                b.meetDate.text = it.date
                b.meetTime.text = it.time
                b.meetPlace.text = it.place
            }

            showInitialView()

            b.backBtn.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            b.editBtn.setOnClickListener {
                val editMeetBottomSheet = EditMeetBottomSheet()
                val args = Bundle().apply {
                    putString(ARG_CURRENT_TITLE, b.meetTitle.text.toString())
                    putString(ARG_CURRENT_DATE, b.meetDate.text.toString())
                    putString(ARG_CURRENT_TIME, b.meetTime.text.toString())
                    putString(ARG_CURRENT_PLACE, b.meetPlace.text.toString())
                }
                editMeetBottomSheet.arguments = args
                editMeetBottomSheet.show(childFragmentManager, EditMeetBottomSheet.TAG)
            }

            b.recordBtn.setOnClickListener {
                Toast.makeText(requireContext(), "녹음하기 기능 준비 중", Toast.LENGTH_SHORT).show()
            }

            b.uploadBtn.setOnClickListener {
                Log.d(TAG, "회의록 첨부하기 버튼 클릭됨 - BottomSheet 표시")
                val currentTitle = b.meetTitle.text.toString()
                val uploadBottomSheet = UploadBottomSheet.newInstance(currentTitle)
                // 여기서 childFragmentManager를 사용했으므로, 리스너도 childFragmentManager에 등록해야 함
                uploadBottomSheet.show(childFragmentManager, UploadBottomSheet.TAG)
            }
        } ?: Log.e(TAG, "onViewCreated에서 binding이 null입니다.")

    }

    // 초기 상태 UI
    private fun showInitialView() {
        binding?.apply {
            textViewMinutesPlaceholder.visibility = View.VISIBLE
            recordBtn.visibility = View.VISIBLE
            uploadBtn.visibility = View.VISIBLE
            summerizedText.visibility = View.GONE
            Log.d(TAG, "초기 뷰 상태 설정됨")
        }
    }

    // 업로드 성공 후 UI
    private fun showSummaryView(summaryText: String) {
        // requireActivity()를 사용하여 Activity의 runOnUiThread 호출
        requireActivity().runOnUiThread {
            binding?.apply {
                textViewMinutesPlaceholder.visibility = View.GONE
                recordBtn.visibility = View.GONE
                uploadBtn.visibility = View.GONE

                summerizedText.visibility = View.VISIBLE
                summerizedText.text = summaryText
                Log.d(TAG, "요약 뷰 표시됨 (on UI thread): $summaryText")
            } ?: Log.e(TAG, "showSummaryView 호출 시 binding이 null입니다. (on UI thread)")
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView 호출됨")
    }

    companion object {
        const val TAG = "MeetDetailFragment"

        @JvmStatic
        fun newInstance(meetItem: MeetListItem) =
            MeetDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_MEET_ITEM, meetItem)
                }
            }
    }
}