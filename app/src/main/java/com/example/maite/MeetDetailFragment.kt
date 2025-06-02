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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey // Added for signature
import com.example.maite.databinding.FragmentMeetDetailBinding
import com.example.maite.model.MeetingDetailResponse
// UserResponse.kt is assumed to be in the same package or properly imported
// For example, if UserResponse.kt is in com.example.maite.model:
// import com.example.maite.model.UserResponse
// import com.example.maite.model.UserResult
// If UserResponse.kt is in com.example.maite:
import com.example.maite.UserResult // Make sure UserResult is correctly imported based on its file location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

private const val ARG_MEETING_ID = "meeting_id"

const val ARG_CURRENT_TITLE = "current_title"
const val ARG_CURRENT_DATE = "current_date"
const val ARG_CURRENT_TIME = "current_time"
const val ARG_CURRENT_PLACE = "current_place"

class MeetDetailFragment : Fragment() {
    private var _binding: FragmentMeetDetailBinding? = null
    private val binding get() = _binding!! // onCreateView 이후 non-null 가정

    private var meetingId: Long? = null
    private var meetingDetail: MeetingDetailResponse? = null

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var audioFilePath: String? = null
    private var aiDialog: AiDialog? = null

    private lateinit var apiService: MaiteApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_MEETING_ID)) {
                meetingId = it.getLong(ARG_MEETING_ID)
            }
        }

        childFragmentManager.setFragmentResultListener(UploadBottomSheet.REQUEST_KEY_UPLOAD, this) { _, bundle ->
            val success = bundle.getBoolean(UploadBottomSheet.BUNDLE_KEY_SUCCESS)
            if (success) {
                val responseBody = bundle.getString(UploadBottomSheet.BUNDLE_KEY_RESPONSE)
                requireActivity().runOnUiThread {
                    _binding?.let { bindingNonNull -> showSummaryView(responseBody ?: "요약본이 생성되었습니다.") }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMeetDetailBinding.inflate(inflater, container, false)
        apiService = MaiteRetrofitClient.getInstance(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        aiDialog = AiDialog(requireContext()).apply {
            onAiCardViewClick = {
                if (isRecording) {
                    stopRecording()
                    this.dismiss()
                }
            }
        }

        _binding?.let { b ->
            meetingId?.let { id ->
                if (id != -1L && id != 0L) {
                    loadMeetingDetails(id)
                } else {
                    Log.e(TAG, "Invalid Meeting ID received: $id")
                    Toast.makeText(requireContext(), "회의 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    showInitialView()
                }
            } ?: run {
                Log.e(TAG, "Meeting ID not provided to MeetDetailFragment.")
                Toast.makeText(requireContext(), "회의 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                showInitialView()
            }

            b.backBtn.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            b.editBtn.setOnClickListener {
                meetingDetail?.let { detail ->
                    val editMeetBottomSheet = EditMeetBottomSheet()
                    val args = Bundle().apply {
                        putString(ARG_CURRENT_TITLE, detail.title)
                        putString(ARG_CURRENT_DATE, detail.meetingDate)
                        putString(ARG_CURRENT_TIME, detail.meetingTime)
                        putString(ARG_CURRENT_PLACE, detail.address)
                    }
                    editMeetBottomSheet.arguments = args
                    editMeetBottomSheet.show(childFragmentManager, EditMeetBottomSheet.TAG)
                } ?: Toast.makeText(requireContext(), "회의 상세 정보를 로드 중입니다.", Toast.LENGTH_SHORT).show()
            }

            b.recordBtn.setOnClickListener {
                Toast.makeText(requireContext(), "녹음하기 기능 준비 중", Toast.LENGTH_SHORT).show()
            }

            b.uploadBtn.setOnClickListener {
                val currentTitle = meetingDetail?.title ?: b.meetTitle.text.toString()
                if (currentTitle.isBlank() && meetingDetail == null) {
                    Toast.makeText(requireContext(), "회의 제목을 불러오는 중입니다...", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val uploadBottomSheet = UploadBottomSheet.newInstance(currentTitle)
                uploadBottomSheet.show(childFragmentManager, UploadBottomSheet.TAG)
            }

            showAiButtonTooltip()
            b.aiCardView.setOnClickListener {
                hideAiButtonTooltip()
                if (checkRecordingPermission()) {
                    if (!isRecording) {
                        aiDialog?.show()
                        startRecording()
                    }
                } else {
                    requestRecordingPermission()
                }
            }
            view.postDelayed({ hideAiButtonTooltip() }, 5000)
        } ?: Log.e(TAG, "Binding is null in onViewCreated.")
    }

    private fun loadMeetingDetails(id: Long) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getMeetingDetail(id)
                }
                if (response.isSuccessful) {
                    meetingDetail = response.body()
                    meetingDetail?.let { detail ->
                        updateUiWithMeetingDetails(detail)
                    } ?: run {
                        Log.e(TAG, "Meeting detail response body is null for ID: $id")
                        Toast.makeText(requireContext(), "회의 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                        showInitialView()
                    }
                } else {
                    Log.e(TAG, "Failed to load meeting details: ${response.code()} - ${response.errorBody()?.string()} for ID: $id")
                    Toast.makeText(requireContext(), "회의 정보 로드 실패: ${response.message()}", Toast.LENGTH_SHORT).show()
                    showInitialView()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading meeting details for ID: $id", e)
                Toast.makeText(requireContext(), "회의 정보 로드 중 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                showInitialView()
            }
        }
    }

    private fun formatDateForDisplay(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            Log.e(TAG, "Date format conversion error for '$dateString'", e)
            dateString
        }
    }

    private fun updateUiWithMeetingDetails(detail: MeetingDetailResponse) {
        _binding?.apply {
            meetTitle.text = detail.title
            meetDate.text = formatDateForDisplay(detail.meetingDate)
            meetTime.text = detail.meetingTime
            meetPlace.text = detail.address ?: "장소가 정해지지 않았습니다."

            Log.d(TAG, "Updating participant images. Count: ${detail.participantEmails.size}")

            while (participantsLayout.childCount > 1) {
                participantsLayout.removeViewAt(participantsLayout.childCount - 1)
            }

            if (detail.participantEmails.isEmpty()) {
                Log.d(TAG, "No participants found.")
                profileImg.visibility = View.GONE
            } else {
                Log.d(TAG, "Participants found: ${detail.participantEmails}")
                profileImg.visibility = View.VISIBLE

                val firstEmail = detail.participantEmails[0]
                loadProfileImage(firstEmail, profileImg)

                if (detail.participantEmails.size > 1) {
                    for (i in 1 until detail.participantEmails.size) {
                        val email = detail.participantEmails[i]
                        Log.d(TAG, "Adding dynamic ImageView for participant ${i + 1}: $email")
                        val imageView = ImageView(requireContext())
                        val imageSizeInPx = (30 * resources.displayMetrics.density).toInt()
                        val marginEndInPx = (8 * resources.displayMetrics.density).toInt()
                        val layoutParams = LinearLayout.LayoutParams(imageSizeInPx, imageSizeInPx).apply {
                            this.marginEnd = marginEndInPx
                        }
                        imageView.layoutParams = layoutParams
                        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                        loadProfileImage(email, imageView)

                        imageView.setOnClickListener {
                            Toast.makeText(requireContext(), "참여자: $email", Toast.LENGTH_SHORT).show()
                        }
                        participantsLayout.addView(imageView)
                        Log.d(TAG, "Dynamic ImageView added for $email. Child count: ${participantsLayout.childCount}")
                    }
                }
            }
            participantsLayout.requestLayout()

            if (!detail.textSum.isNullOrBlank()) {
                showSummaryView(detail.textSum)
            } else if (!detail.recordText.isNullOrBlank()) {
                showSummaryView(detail.recordText)
            } else {
                showInitialViewMinutes()
            }
        } ?: Log.e(TAG, "Binding is null in updateUiWithMeetingDetails, cannot update UI.")
    }

    private fun loadProfileImage(email: String, imageView: ImageView) {
        lifecycleScope.launch {
            try {
                // Attempt to get user info (which includes profileImageUrl) from API
                val response = withContext(Dispatchers.IO) {
                    apiService.searchUsers(email) // This should return Response<UserResponse>
                }

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val userList: List<UserResult>? = response.body()?.result
                    val userInfo: UserResult? = userList?.firstOrNull { it.email == email }
                        ?: userList?.firstOrNull() // Fallback to first if no exact email match

                    val profileImageUrl = userInfo?.profileImageUrl

                    if (!profileImageUrl.isNullOrBlank()) {
                        Glide.with(this@MeetDetailFragment)
                            .load(profileImageUrl)
                            .apply(RequestOptions.circleCropTransform()) // Apply circle crop
                            .skipMemoryCache(true) // Similar to ProfileFragment
                            .signature(ObjectKey(System.currentTimeMillis().toString())) // Similar to ProfileFragment for cache busting
                            .placeholder(R.drawable.img_profile_default)
                            .error(R.drawable.img_profile_default)
                            .into(imageView)
                    } else {
                        // URL is blank or user not found, set default image
                        imageView.setImageResource(R.drawable.img_profile_default)
                        Log.d(TAG, "Profile image URL is null or blank for $email (or user not found), using default.")
                    }
                } else {
                    // API call failed or was not successful
                    imageView.setImageResource(R.drawable.img_profile_default)
                    Log.e(TAG, "Failed to fetch user details for $email or API error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                // Exception during the process (e.g., network error)
                imageView.setImageResource(R.drawable.img_profile_default)
                Log.e(TAG, "Exception loading profile image for $email: ${e.message}", e)
            }
        }
    }


    private fun showAiButtonTooltip() {
        _binding?.aiButtonTooltip?.apply {
            visibility = View.VISIBLE; alpha = 0f
            animate().alpha(1f).setDuration(300).start()
        }
    }

    private fun hideAiButtonTooltip() {
        _binding?.aiButtonTooltip?.apply {
            animate().alpha(0f).setDuration(300).withEndAction { visibility = View.GONE }.start()
        }
    }

    private fun startRecording() {
        try {
            audioFilePath = "${requireActivity().externalCacheDir?.absolutePath}/audio_record_${System.currentTimeMillis()}.mp3"
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(requireContext()) else MediaRecorder()
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
                    updateRecordingUI(true)
                } catch (e: IOException) {
                    Log.e(TAG, "녹음 준비 실패: ${e.message}")
                    Toast.makeText(requireContext(), "녹음 준비에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    aiDialog?.dismiss()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "녹음 시작 실패: ${e.message}")
            Toast.makeText(requireContext(), "녹음을 시작할 수 없습니다.", Toast.LENGTH_SHORT).show()
            aiDialog?.dismiss()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply { stop(); reset(); release() }
            mediaRecorder = null
            isRecording = false
            Toast.makeText(requireContext(), "녹음이 저장되었습니다: $audioFilePath", Toast.LENGTH_LONG).show()
            updateRecordingUI(false)
            processRecordedAudio(audioFilePath)
        } catch (e: Exception) {
            Log.e(TAG, "녹음 중지 실패: ${e.message}")
        }
    }

    private fun updateRecordingUI(isRecording: Boolean) {
        // UI 업데이트 (예: 버튼 아이콘 변경 등)
    }

    private fun processRecordedAudio(filePath: String?) {
        filePath?.let { Log.d(TAG, "녹음 파일 처리 중: $it") }
    }

    private fun checkRecordingPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordingPermission() {
        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                aiDialog?.show()
                startRecording()
            } else {
                Toast.makeText(requireContext(), "녹음을 위해서는 오디오 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showInitialView() {
        _binding?.apply {
            meetTitle.text = "회의 정보 로드 실패"
            meetDate.text = ""
            meetTime.text = ""
            meetPlace.text = ""
            profileImg.visibility = View.GONE
            while (participantsLayout.childCount > 1) {
                participantsLayout.removeViewAt(participantsLayout.childCount - 1)
            }
            showInitialViewMinutes()
        }
    }

    private fun showInitialViewMinutes() {
        _binding?.apply {
            textViewMinutesPlaceholder.visibility = View.VISIBLE
            recordBtn.visibility = View.VISIBLE
            uploadBtn.visibility = View.VISIBLE
            summerizedText.visibility = View.GONE
        }
    }

    private fun showSummaryView(summaryText: String) {
        requireActivity().runOnUiThread {
            _binding?.apply {
                textViewMinutesPlaceholder.visibility = View.GONE
                recordBtn.visibility = View.GONE
                uploadBtn.visibility = View.GONE
                summerizedText.visibility = View.VISIBLE
                summerizedText.text = summaryText
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isRecording) {
            stopRecording()
            aiDialog?.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaRecorder?.release()
        mediaRecorder = null
        aiDialog?.dismiss()
        aiDialog = null
        _binding = null
    }

    companion object {
        const val TAG = "MeetDetailFragment"
        const val REQUEST_RECORD_AUDIO_PERMISSION = 200

        @JvmStatic
        fun newInstance(meetingId: Long): MeetDetailFragment {
            return MeetDetailFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_MEETING_ID, meetingId)
                }
            }
        }
    }
}