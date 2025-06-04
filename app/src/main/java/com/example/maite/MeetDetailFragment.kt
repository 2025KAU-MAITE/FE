package com.example.maite

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.example.maite.databinding.FragmentMeetDetailBinding
import com.example.maite.model.ClovaSummaryResponse
import com.example.maite.model.MeetingDetailResponse
import com.example.maite.UserResult
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
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
    private val binding get() = _binding!!

    private var meetingId: Long? = null
    private var meetingDetail: MeetingDetailResponse? = null

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var audioFilePath: String? = null
    private var aiDialog: AiDialog? = null

    private var responseMediaPlayer: MediaPlayer? = null
    private var currentResponseAudioPath: String? = null

    private lateinit var apiService: MaiteApiService

    private var isSubscribed = false

    private var currentTabPosition = 0

    private var hasSummary = false
    private var hasTranscript = false
    private var summaryContent: String? = null
    private var transcriptContent: String? = null

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
                requireActivity().runOnUiThread {
                    meetingId?.let { id ->
                        loadMeetingDetails(id)
                    }
                }
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

        val preferencesUtil = PreferencesUtil(requireContext())
        isSubscribed = preferencesUtil.isSubscribed()

        aiDialog = AiDialog(requireContext()).apply {
            onAiCardViewClick = {
                if (isRecording) {
                    stopRecording()
                    this.startProcessing()
                }
            }
        }

        _binding?.let { b ->
            meetingId?.let { id ->
                if (id != -1L && id != 0L) {
                    loadMeetingDetails(id)
                } else {
                    showInitialView()
                }
            } ?: run {
                showInitialView()
            }

            if (isSubscribed) {
                setupSubscribedUI()
            } else {
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
                }
            }

            b.recordBtn.setOnClickListener {
            }

            b.uploadBtn.setOnClickListener {
                val currentTitle = meetingDetail?.title ?: b.meetTitle.text.toString()
                val currentMeetingId = meetingId

                if (currentTitle.isBlank() && meetingDetail == null) {
                    return@setOnClickListener
                }

                if (currentMeetingId == null) {
                    return@setOnClickListener
                }

                val uploadBottomSheet = UploadBottomSheet.newInstance(currentTitle, currentMeetingId)
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
        }
    }

    private fun setupSubscribedUI() {
        binding.apply {
            textViewMinutesPlaceholder.visibility = View.VISIBLE
            recordBtn.visibility = View.VISIBLE
            uploadBtn.visibility = View.VISIBLE

            summerizedText.text = ""
            summerizedText.visibility = View.GONE

            tabLayout.visibility = View.VISIBLE

            val firstTab = tabLayout.getTabAt(0)
            firstTab?.select()
            firstTab?.view?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.subColor))

            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.let {
                        currentTabPosition = it.position
                        updateUiForTabSelection(currentTabPosition, hasSummary, hasTranscript)
                        tab.view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.subColor))
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    tab?.view?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })

        }
    }

    private fun updateUiForTabSelection(position: Int, hasValidSummary: Boolean = false, hasValidTranscript: Boolean = false) {
        binding.apply {
            summerizedText.text = ""
            summerizedText.visibility = View.GONE

            val hasContent = when (position) {
                0 -> hasValidSummary
                1 -> hasValidTranscript
                else -> false
            }

            recordBtn.visibility = if (hasContent) View.GONE else View.VISIBLE
            uploadBtn.visibility = if (hasContent) View.GONE else View.VISIBLE

            when (position) {
                0 -> {
                    transcriptScrollView.visibility = View.GONE

                    if (hasValidSummary) {
                        summaryScrollView.visibility = View.VISIBLE
                        textViewMinutesPlaceholder.visibility = View.GONE
                    } else {
                        summaryScrollView.visibility = View.GONE
                        textViewMinutesPlaceholder.text = "등록된 요약본이 없어요"
                        textViewMinutesPlaceholder.visibility = View.VISIBLE
                    }
                }
                1 -> {
                    summaryScrollView.visibility = View.GONE

                    if (hasValidTranscript) {
                        transcriptScrollView.visibility = View.VISIBLE
                        textViewMinutesPlaceholder.visibility = View.GONE
                    } else {
                        transcriptScrollView.visibility = View.GONE
                        textViewMinutesPlaceholder.text = "등록된 회의록이 없어요"
                        textViewMinutesPlaceholder.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun loadMeetingDetails(id: Long) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getMeetingDetail(id)
                }
                if (response.isSuccessful) {
                    meetingDetail = response.body()
                    Log.d("MeetDetail", "API Response: ${meetingDetail?.title}, ${meetingDetail?.meetingDate}")

                    meetingDetail?.let { detail ->
                        // 구독 상태와 관계없이 기본 정보는 항상 업데이트
                        updateUiWithMeetingDetails(detail)

                        // 구독 상태에 따른 추가 UI 업데이트
                        if (isSubscribed) {
                            // 구독자용 추가 기능 업데이트
                            updateUiForTabSelection(currentTabPosition, hasSummary, hasTranscript)
                        }
                    } ?: run {
                        showInitialView()
                    }
                } else {
                    showInitialView()
                }
            } catch (e: Exception) {
                showInitialView()
            }
        }
    }

    private fun handleStandardApiResponse(response: Response<ResponseBody>) {
        if (response.isSuccessful) {
            try {
                val responseBody = response.body()?.string() ?: "응답 내용이 없습니다."

                meetingId?.let { id ->
                    loadMeetingDetails(id)
                }

            } catch (e: Exception) {
            }
        } else {
        }
    }

    private fun handleClovaApiResponse(response: Response<ClovaSummaryResponse>) {
        if (!response.isSuccessful) {
            showApiErrorMessage("API 호출 실패: ${response.message()}")
        }
    }

    private fun showApiErrorMessage(message: String) {
        binding.apply {
            summaryTextView.text = "요약본을 불러올 수 없습니다. $message"
            transcriptTextView.text = "회의록을 불러올 수 없습니다. $message"
        }
    }

    private fun formatDateForDisplay(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }

    private fun updateUiWithMeetingDetails(detail: MeetingDetailResponse) {
        Log.d("MeetDetail", "Updating UI with: ${detail.title}, ${detail.meetingDate}, ${detail.meetingTime}")
        _binding?.apply {
            meetTitle.text = detail.title
            meetDate.text = formatDateForDisplay(detail.meetingDate)
            meetTime.text = "${detail.meetingTime} - ${detail.meetingEndTime}"
            meetPlace.text = detail.address ?: "장소가 정해지지 않았습니다."

            while (participantsLayout.childCount > 1) {
                participantsLayout.removeViewAt(participantsLayout.childCount - 1)
            }

            if (detail.participantEmails.isEmpty()) {
                profileImg.visibility = View.GONE
            } else {
                profileImg.visibility = View.VISIBLE

                val firstEmail = detail.participantEmails[0]
                loadProfileImage(firstEmail, profileImg)

                if (detail.participantEmails.size > 1) {
                    for (i in 1 until detail.participantEmails.size) {
                        val email = detail.participantEmails[i]
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
                        }
                        participantsLayout.addView(imageView)
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
        }
    }

    private fun loadProfileImage(email: String, imageView: ImageView) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.searchUsers(email)
                }

                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val userList: List<UserResult>? = response.body()?.result
                    val userInfo: UserResult? = userList?.firstOrNull { it.email == email }
                        ?: userList?.firstOrNull()

                    val profileImageUrl = userInfo?.profileImageUrl

                    if (!profileImageUrl.isNullOrBlank()) {
                        Glide.with(this@MeetDetailFragment)
                            .load(profileImageUrl)
                            .apply(RequestOptions.circleCropTransform())
                            .skipMemoryCache(true)
                            .signature(ObjectKey(System.currentTimeMillis().toString()))
                            .placeholder(R.drawable.img_profile_default)
                            .error(R.drawable.img_profile_default)
                            .into(imageView)
                    } else {
                        imageView.setImageResource(R.drawable.img_profile_default)
                    }
                } else {
                    imageView.setImageResource(R.drawable.img_profile_default)
                }
            } catch (e: Exception) {
                imageView.setImageResource(R.drawable.img_profile_default)
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
                    updateRecordingUI(true)
                } catch (e: IOException) {
                    aiDialog?.dismiss()
                }
            }
        } catch (e: Exception) {
            aiDialog?.dismiss()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply { stop(); reset(); release() }
            mediaRecorder = null
            isRecording = false
            updateRecordingUI(false)

            processRecordedAudio(audioFilePath)
        } catch (e: Exception) {
        }
    }

    private fun updateRecordingUI(isRecording: Boolean) {
    }

    private fun processRecordedAudio(filePath: String?) {
        filePath?.let { path ->

            lifecycleScope.launch {
                try {

                    val file = File(path)
                    if (!file.exists()) {
                        aiDialog?.dismiss()
                        return@launch
                    }

                    val requestFile = file.asRequestBody("audio/mp3".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                    val response = withContext(Dispatchers.IO) {
                        apiService.getAiReply(body)
                    }

                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            val audioBytes = responseBody.bytes()

                            saveAndAutoPlayAudioResponse(audioBytes)
                        } else {
                            aiDialog?.dismiss()
                        }
                    } else {
                        aiDialog?.dismiss()
                    }

                    file.delete()

                } catch (e: Exception) {
                    aiDialog?.dismiss()
                }
            }
        }
    }

    private fun showLoadingState() {
        _binding?.apply {
        }
    }

    private fun hideLoadingState() {
        _binding?.apply {
        }
    }

    private fun saveAndAutoPlayAudioResponse(audioBytes: ByteArray) {
        try {
            val responseAudioPath = "${requireActivity().externalCacheDir?.absolutePath}/ai_response_${System.currentTimeMillis()}.mp3"
            val responseFile = File(responseAudioPath)

            responseFile.writeBytes(audioBytes)

            currentResponseAudioPath = responseAudioPath

            autoPlayAudioResponse(responseAudioPath)

        } catch (e: Exception) {
        }
    }

    private fun autoPlayAudioResponse(audioPath: String) {
        try {
            stopAudioResponse()

            responseMediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                prepareAsync()
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.start()

                    aiDialog?.startAiResponse(mediaPlayer)

                    setOnCompletionListener {

                        aiDialog?.stopAiResponse()

                        cleanupResponseAudio()

                        Handler(Looper.getMainLooper()).postDelayed({
                            aiDialog?.dismiss()
                        }, 500)
                    }
                }
                setOnErrorListener { _, what, extra ->
                    aiDialog?.stopAiResponse()
                    aiDialog?.dismiss()
                    true
                }
            }

        } catch (e: Exception) {
            aiDialog?.dismiss()
        }
    }

    private fun stopAudioResponse() {
        responseMediaPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            } catch (e: Exception) {
            }
        }
        responseMediaPlayer = null

        aiDialog?.stopAiResponse()
    }

    private fun cleanupResponseAudio() {
        currentResponseAudioPath?.let { path ->
            try {
                File(path).delete()
            } catch (e: Exception) {
            }
        }
        currentResponseAudioPath = null
    }

    private fun uploadAudioFileForSubscriber(filePath: String, meetingId: Long, topic: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    withContext(Dispatchers.Main) {
                    }
                    return@launch
                }

                val requestFile = file.asRequestBody("audio/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val response = apiService.uploadAudioSummaryClova(topic, meetingId, filePart)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val clovaResponse = response.body()
                        if (clovaResponse != null && clovaResponse.isSuccess) {

                            loadMeetingDetails(meetingId)
                        } else {
                        }
                    } else {
                        handleClovaApiResponse(response)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                }
            }
        }
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

        stopAudioResponse()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaRecorder?.release()
        mediaRecorder = null

        stopAudioResponse()

        cleanupResponseAudio()

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