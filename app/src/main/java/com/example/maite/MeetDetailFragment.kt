package com.example.maite

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
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
import java.io.IOException

private const val ARG_MEET_ITEM = "meet_item"

const val ARG_CURRENT_TITLE = "current_title"
const val ARG_CURRENT_DATE = "current_date"
const val ARG_CURRENT_TIME = "current_time"
const val ARG_CURRENT_PLACE = "current_place"

class MeetDetailFragment : Fragment() {
    private var _binding: FragmentMeetDetailBinding? = null
    private val binding get() = _binding

    private var meetItem: MeetListItem? = null

    // 녹음 관련 변수
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var audioFilePath: String? = null

    // AI 다이얼로그
    private var aiDialog: AiDialog? = null

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

        // AI 다이얼로그 초기화
        aiDialog = AiDialog(requireContext())
        aiDialog?.onAiCardViewClick = {
            // AI 다이얼로그 내의 aiCardView 클릭 시 녹음 중지
            if (isRecording) {
                stopRecording()
                aiDialog?.dismiss()
            }
        }

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

            showAiButtonTooltip()

            b.aiCardView.setOnClickListener {
                hideAiButtonTooltip()

                // 녹음 권한 확인 후 AI 다이얼로그 표시 및 녹음 시작
                if (checkRecordingPermission()) {
                    if (!isRecording) {
                        // AI 다이얼로그 표시 및 녹음 시작
                        aiDialog?.show()
                        startRecording()
                    }
                } else {
                    requestRecordingPermission()
                }
            }

            view.postDelayed({
                hideAiButtonTooltip()
            }, 5000)
        } ?: Log.e(TAG, "onViewCreated에서 binding이 null입니다.")

    }

    private fun showAiButtonTooltip() {
        binding?.aiButtonTooltip?.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate().alpha(1f).setDuration(300).start()
        }
    }

    private fun hideAiButtonTooltip() {
        binding?.aiButtonTooltip?.apply {
            animate().alpha(0f).setDuration(300).withEndAction {
                visibility = View.GONE
            }.start()
        }
    }

    private fun startRecording() {
        try {
            // 녹음 파일 경로 설정
            audioFilePath = "${requireActivity().externalCacheDir?.absolutePath}/audio_record_${System.currentTimeMillis()}.mp3"

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(requireContext())
            } else {
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFilePath)

                try {
                    prepare()
                    start()
                    isRecording = true

                    Toast.makeText(requireContext(), "녹음이 시작되었습니다.", Toast.LENGTH_SHORT).show()
                    // UI 업데이트: 녹음 중임을 표시
                    updateRecordingUI(true)
                } catch (e: IOException) {
                    Log.e(TAG, "녹음 준비 실패: ${e.message}")
                    Toast.makeText(requireContext(), "녹음 준비에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    aiDialog?.dismiss() // 녹음 실패 시 다이얼로그 닫기
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "녹음 시작 실패: ${e.message}")
            Toast.makeText(requireContext(), "녹음을 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
            aiDialog?.dismiss() // 녹음 실패 시 다이얼로그 닫기
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            isRecording = false

            Toast.makeText(requireContext(), "녹음이 저장되었습니다: $audioFilePath", Toast.LENGTH_LONG).show()
            // UI 업데이트: 녹음 종료 표시
            updateRecordingUI(false)

            // 필요하다면 녹음된 오디오를 처리 (예: 서버 업로드, 회의록 생성 등)
            processRecordedAudio(audioFilePath)

        } catch (e: Exception) {
            Log.e(TAG, "녹음 중지 실패: ${e.message}")
            Toast.makeText(requireContext(), "녹음 중지에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateRecordingUI(isRecording: Boolean) {
        binding?.aiCardView?.let { button ->
            // 녹음 중일 때 버튼 모양이나 색상 변경
            // 여러분의 앱에 맞는 리소스로 변경하세요
            // button.setImageResource(if (isRecording) R.drawable.ic_recording else R.drawable.btn_ai)
        }

        // 녹음 중 상태를 사용자에게 알리는 토스트 메시지
        if (isRecording) {
            Toast.makeText(requireContext(), "녹음 중...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processRecordedAudio(filePath: String?) {
        filePath?.let {
            Log.d(TAG, "녹음 파일 처리 중: $it")
        }
    }

    private fun checkRecordingPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordingPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_RECORD_AUDIO_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한 획득 성공, AI 다이얼로그 표시 및 녹음 시작
                aiDialog?.show()
                startRecording()
            } else {
                Toast.makeText(requireContext(), "녹음을 위해서는 오디오 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
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

    override fun onStop() {
        super.onStop()
        // 앱이 중지될 때 녹음 중이었다면 녹음 중지
        if (isRecording) {
            stopRecording()
            aiDialog?.dismiss() // 앱 중지 시 다이얼로그도 닫기
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        aiDialog = null // 메모리 누수 방지
        Log.d(TAG, "onDestroyView 호출됨")
    }

    companion object {
        const val TAG = "MeetDetailFragment"
        const val REQUEST_RECORD_AUDIO_PERMISSION = 200

        @JvmStatic
        fun newInstance(meetItem: MeetListItem) =
            MeetDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_MEET_ITEM, meetItem)
                }
            }
    }
}