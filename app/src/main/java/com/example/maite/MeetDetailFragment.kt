package com.example.maite

import android.Manifest
import android.app.Dialog // Keep for showAddressInputDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer // Re-added for AI voice response
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler // Re-added for AI voice response completion
import android.os.Looper // Re-added for AI voice response completion
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.example.maite.databinding.FragmentMeetDetailBinding
// import com.example.maite.model.ClovaSummaryResponse // Keep if API service uses it for upload
import com.example.maite.model.MeetingDetailResponse
import com.example.maite.UserResult
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
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

private const val ARG_EDIT_MEETING_ID = "edit_meeting_id"

class MeetDetailFragment : Fragment() {
    private var _binding: FragmentMeetDetailBinding? = null
    private val binding get() = _binding!!

    private var meetingId: Long? = null
    private var meetingDetail: MeetingDetailResponse? = null

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var audioFilePath: String? = null

    // Dialogs for different recording flows
    private var aiDialog: AiDialog? = null // For floating button (AI voice response)
    private var recordDialog: RecordDialog? = null // For recordBtn (upload flow)

    // For AI Voice Response Playback
    private var responseMediaPlayer: MediaPlayer? = null
    private var currentResponseAudioPath: String? = null

    private lateinit var apiService: MaiteApiService
    private var uploadJob: Job? = null // For the upload flow

    private var recordingForUploadFlow: Boolean = false // Flag to distinguish recording type

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
                        Log.d(TAG, "UploadBottomSheet success. Reloading details for meeting ID: $id")
                        loadMeetingDetails(id)
                    }
                }
            }
        }

        childFragmentManager.setFragmentResultListener(EditMeetBottomSheet.REQUEST_KEY, this) { _, bundle ->
            val success = bundle.getBoolean(EditMeetBottomSheet.BUNDLE_KEY_SUCCESS, false)
            if (success) {
                meetingId?.let { id ->
                    loadMeetingDetails(id)
                    val resultBundle = Bundle().apply {
                        putBoolean("meeting_update_success", true)
                        putLong("updated_meeting_id", id)
                    }
                    parentFragmentManager.setFragmentResult("meeting_update_result", resultBundle)
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
        if (context == null) return

        val preferencesUtil = PreferencesUtil(requireContext())
        isSubscribed = preferencesUtil.isSubscribed()

        // Initialize AiDialog (for floating button - AI voice response)
        aiDialog = AiDialog(requireContext()).apply {
            onAiCardViewClick = { // Button inside AiDialog
                if (isRecording) {
                    // recordingForUploadFlow should be false if AiDialog was used
                    stopRecording()
                }
            }
        }

        // Initialize RecordDialog (for recordBtn - upload flow)
        recordDialog = RecordDialog(requireContext()).apply {
            onRecordDialogClick = { // Button inside RecordDialog
                if (isRecording) {
                    // recordingForUploadFlow should be true if RecordDialog was used
                    stopRecording()
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
            // Initial UI setup based on subscription status
            if (isSubscribed) {
                setupSubscribedUI()
            } else {
                if (meetingDetail == null) {
                    showInitialView()
                } else {
                    updateUiWithMeetingDetails(meetingDetail!!)
                }
            }

            b.backBtn.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            b.editBtn.setOnClickListener {
                meetingDetail?.let { detail ->
                    val editMeetBottomSheet = EditMeetBottomSheet()
                    // ... (args setup)
                    val args = Bundle().apply {
                        putLong(ARG_EDIT_MEETING_ID, meetingId ?: -1L)
                        putString(ARG_CURRENT_TITLE, detail.title)
                        putString(ARG_CURRENT_DATE, detail.meetingDate)
                        putString(ARG_CURRENT_TIME, "${detail.meetingTime} ~ ${detail.meetingEndTime}")
                        putString(ARG_CURRENT_PLACE, detail.address)
                    }
                    editMeetBottomSheet.arguments = args
                    editMeetBottomSheet.show(childFragmentManager, EditMeetBottomSheet.TAG)
                }
            }

            // Record Button (for Upload Flow)
            b.recordBtn.setOnClickListener {
                if (meetingDetail == null && meetingId == null) {
                    Toast.makeText(context, "회의 정보를 먼저 로드하거나 생성해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (checkRecordingPermission()) {
                    if (!isRecording) {
                        recordingForUploadFlow = true // Set flag for UPLOAD flow
                        recordDialog?.show()
                        startRecording()
                    }
                } else {
                    requestRecordingPermission()
                }
            }

            b.uploadBtn.setOnClickListener {
                // ... (uploadBottomSheet logic remains the same)
                val currentTitle = meetingDetail?.title ?: b.meetTitle.text.toString()
                val currentMeetingId = meetingId
                if ((currentTitle.isBlank()) && meetingDetail == null) {
                    Toast.makeText(requireContext(), "회의 정보를 먼저 입력하거나 생성해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (currentMeetingId == null || currentMeetingId == -1L) {
                    Toast.makeText(requireContext(), "회의 정보를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val uploadBottomSheet = UploadBottomSheet.newInstance(currentTitle, currentMeetingId)
                uploadBottomSheet.show(childFragmentManager, UploadBottomSheet.TAG)
            }

            // Floating AI Button (for AI Voice Response Flow)
            showAiButtonTooltip()
            b.aiCardView.setOnClickListener {
                hideAiButtonTooltip()
                if (meetingDetail == null && meetingId == null) { // Prevent use if no meeting context
                    Toast.makeText(context, "회의 정보를 먼저 로드하거나 생성해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (checkRecordingPermission()) {
                    if (!isRecording) {
                        recordingForUploadFlow = false // Set flag for AI RESPONSE flow
                        aiDialog?.show()
                        startRecording()
                    }
                } else {
                    requestRecordingPermission()
                }
            }
            view.postDelayed({ hideAiButtonTooltip() }, 5000)

            b.imageView3.setOnClickListener {
                showAddressInputDialog()
            }
        }
    }

    // ... showAddressInputDialog, updateButtonAppearance, updateMeetingAddress ...
    // ... setupSubscribedUI, updateUiForTabSelection, loadMeetingDetails, formatDateForDisplay ...
    // ... updateUiWithMeetingDetails, loadProfileImage, showAiButtonTooltip, hideAiButtonTooltip ...
    // (These functions remain largely the same, ensure context checks)

    private fun showAddressInputDialog() {
        val currentMeetingId = meetingId ?: return
        if (!isAdded || context == null) return

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_address_input)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val addressEditText = dialog.findViewById<EditText>(R.id.addressEditText)
        val saveButton = dialog.findViewById<Button>(R.id.saveButton)
        updateButtonAppearance(saveButton, false)

        addressEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateButtonAppearance(saveButton, s?.isNotEmpty() == true)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        saveButton.setOnClickListener {
            val address = addressEditText.text.toString().trim()
            if (address.isNotEmpty()) {
                updateMeetingAddress(currentMeetingId, address)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun updateButtonAppearance(button: Button, isActive: Boolean) {
        if (!isAdded || context == null) return
        if (isActive) {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.mainColor))
            button.setTextColor(Color.WHITE)
        } else {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.btn_inactive))
            button.setTextColor(Color.BLACK)
        }
    }

    private fun updateMeetingAddress(id: Long, address: String) {
        if (!isAdded || context == null) return
        lifecycleScope.launch {
            try {
                val request = SetAddressRequest(address)
                val response = withContext(Dispatchers.IO) { apiService.setMyMeetingAddress(id, request) }
                if (response.isSuccessful) {
                    binding.meetPlace.text = address
                    loadMeetingDetails(id)
                } else {
                    Toast.makeText(context, "주소 업데이트 실패: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating meeting address", e)
                Toast.makeText(context, "주소 업데이트 중 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSubscribedUI() {
        if (!isAdded || context == null) return
        binding.apply {
            summerizedText.visibility = View.GONE
            tabLayout.visibility = View.VISIBLE
            val firstTab = tabLayout.getTabAt(0)
            firstTab?.select()
            context?.let { ctx ->
                firstTab?.view?.setBackgroundColor(ContextCompat.getColor(ctx, R.color.subColor))
            }
            updateUiForTabSelection(currentTabPosition, hasSummary, hasTranscript)
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.let {
                        currentTabPosition = it.position
                        updateUiForTabSelection(currentTabPosition, hasSummary, hasTranscript)
                        context?.let { ctx ->
                            it.view.setBackgroundColor(ContextCompat.getColor(ctx, R.color.subColor))
                        }
                    }
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    if (!isAdded) return
                    context?.let { ctx ->
                        tab?.view?.setBackgroundColor(ContextCompat.getColor(ctx, R.color.white))
                    }
                }
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }
    }

    private fun updateUiForTabSelection(position: Int, hasValidSummary: Boolean, hasValidTranscript: Boolean) {
        if (!isAdded || context == null) return
        binding.apply {
            summerizedText.visibility = View.GONE
            defaultContentLayout.visibility = View.GONE
            summaryScrollView.visibility = View.GONE
            transcriptScrollView.visibility = View.GONE
            textViewMinutesPlaceholder.visibility = View.GONE

            when (position) {
                0 -> {
                    if (hasValidSummary) {
                        summaryScrollView.visibility = View.VISIBLE
                        summaryTextView.text = summaryContent ?: "등록된 요약본이 없어요"
                    } else {
                        textViewMinutesPlaceholder.text = "등록된 요약본이 없어요"
                        textViewMinutesPlaceholder.visibility = View.VISIBLE
                        defaultContentLayout.visibility = View.VISIBLE
                    }
                }
                1 -> {
                    if (hasValidTranscript) {
                        transcriptScrollView.visibility = View.VISIBLE
                        transcriptTextView.text = transcriptContent ?: "등록된 회의록이 없어요"
                    } else {
                        textViewMinutesPlaceholder.text = "등록된 회의록이 없어요"
                        textViewMinutesPlaceholder.visibility = View.VISIBLE
                        defaultContentLayout.visibility = View.VISIBLE
                    }
                }
            }
            val hasContentForCurrentTab = if (position == 0) hasValidSummary else hasValidTranscript
            recordBtn.visibility = if (hasContentForCurrentTab) View.GONE else View.VISIBLE
            uploadBtn.visibility = if (hasContentForCurrentTab) View.GONE else View.VISIBLE
        }
    }

    private fun loadMeetingDetails(id: Long) {
        if (!isAdded || context == null) return
        Log.d(TAG, "Loading meeting details for ID: $id")
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { apiService.getMeetingDetail(id) }
                if (response.isSuccessful) {
                    meetingDetail = response.body()
                    Log.d(TAG, "Successfully loaded meeting details: ${meetingDetail?.title}")
                    meetingDetail?.let { detail ->
                        updateUiWithMeetingDetails(detail)
                    } ?: showInitialView()
                } else {
                    Log.e(TAG, "Failed to load meeting details: ${response.code()} - ${response.message()}")
                    showInitialView()
                    Toast.makeText(context, "회의 정보를 불러오지 못했습니다: ${response.message()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading meeting details", e)
                showInitialView()
                Toast.makeText(context, "회의 정보 로드 중 오류 발생: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun formatDateForDisplay(dateString: String): String {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)?.let {
                SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(it)
            } ?: dateString
        } catch (e: Exception) {
            dateString
        }
    }

    private fun updateUiWithMeetingDetails(detail: MeetingDetailResponse) {
        if (!isAdded || context == null) return
        Log.d(TAG, "Updating UI with meeting details: ${detail.title}")
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
                loadProfileImage(detail.participantEmails[0], profileImg)
                if (detail.participantEmails.size > 1) {
                    for (i in 1 until detail.participantEmails.size) {
                        val email = detail.participantEmails[i]
                        val imageView = ImageView(requireContext()).apply {
                            val imageSize = (30 * resources.displayMetrics.density).toInt()
                            layoutParams = LinearLayout.LayoutParams(imageSize, imageSize).apply { marginEnd = (8 * resources.displayMetrics.density).toInt() }
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        loadProfileImage(email, imageView)
                        participantsLayout.addView(imageView)
                    }
                }
            }
            participantsLayout.requestLayout()

            summaryContent = detail.textSum
            transcriptContent = detail.recordText
            hasSummary = !detail.textSum.isNullOrBlank()
            hasTranscript = !detail.recordText.isNullOrBlank()

            if (isSubscribed) {
                setupSubscribedUI()
            } else {
                tabLayout.visibility = View.GONE
                summaryScrollView.visibility = View.GONE
                transcriptScrollView.visibility = View.GONE
                defaultContentLayout.visibility = View.GONE
                textViewMinutesPlaceholder.visibility = View.GONE
                summerizedText.visibility = View.VISIBLE

                if (hasSummary) {
                    summerizedText.text = detail.textSum
                    recordBtn.visibility = View.GONE
                    uploadBtn.visibility = View.GONE
                } else if (hasTranscript) {
                    summerizedText.text = detail.recordText
                    recordBtn.visibility = View.GONE
                    uploadBtn.visibility = View.GONE
                } else {
                    summerizedText.text = "생성된 요약본 또는 회의록이 없습니다.\nAI 기능을 사용해 회의를 기록하고 요약해보세요!"
                    recordBtn.visibility = View.VISIBLE
                    uploadBtn.visibility = View.VISIBLE
                }
            }
        }
        Log.d(TAG, "UI update finished for: ${detail.title}")
    }

    private fun loadProfileImage(email: String, imageView: ImageView) {
        if (!isAdded || context == null) return
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { apiService.searchUsers(email) }
                if (response.isSuccessful && response.body()?.isSuccess == true) {
                    val user = response.body()?.result?.firstOrNull { it.email == email } ?: response.body()?.result?.firstOrNull()
                    if (!user?.profileImageUrl.isNullOrBlank()) {
                        Glide.with(this@MeetDetailFragment)
                            .load(user!!.profileImageUrl)
                            .apply(RequestOptions.circleCropTransform())
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
                Log.e(TAG, "Error loading profile image", e)
                imageView.setImageResource(R.drawable.img_profile_default)
            }
        }
    }

    private fun showAiButtonTooltip() { _binding?.aiButtonTooltip?.apply { visibility = View.VISIBLE; alpha = 0f; animate().alpha(1f).setDuration(300).start() } }
    private fun hideAiButtonTooltip() { _binding?.aiButtonTooltip?.apply { animate().alpha(0f).setDuration(300).withEndAction { visibility = View.GONE }.start() } }


    private fun startRecording() {
        if (!isAdded || context == null) return
        try {
            audioFilePath = "${requireActivity().externalCacheDir?.absolutePath}/MAITE_REC_${System.currentTimeMillis()}.m4a"
            mediaRecorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(requireContext()) else MediaRecorder()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }
            isRecording = true
            Log.d(TAG, "Recording started: $audioFilePath. For upload flow: $recordingForUploadFlow")
        } catch (e: IOException) {
            Log.e(TAG, "MediaRecorder prepare/start failed", e)
            if (recordingForUploadFlow) recordDialog?.dismiss() else aiDialog?.dismiss()
            Toast.makeText(context, "녹음 시작 실패", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "MediaRecorder setup failed", e)
            if (recordingForUploadFlow) recordDialog?.dismiss() else aiDialog?.dismiss()
            Toast.makeText(context, "녹음 장치 초기화 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        Log.d(TAG, "Attempting to stop recording. For upload flow: $recordingForUploadFlow")
        if (!isRecording && mediaRecorder == null) {
            Log.w(TAG, "StopRecording called but not recording or recorder is null.")
            if (recordingForUploadFlow) recordDialog?.dismiss() else aiDialog?.dismiss()
            cleanupRecordedFile(audioFilePath)
            return
        }
        val currentAudioFilePath = audioFilePath // Capture before it's nulled by cleanup
        isRecording = false // Set recording state to false immediately

        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            Log.d(TAG, "MediaRecorder stopped and released.")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "MediaRecorder stop/reset/release failed (IllegalStateException)", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during MediaRecorder stop/reset/release", e)
        } finally {
            mediaRecorder = null // Ensure recorder is nullified

            if (currentAudioFilePath != null && File(currentAudioFilePath).exists()) {
                Log.d(TAG, "Recorded file exists: $currentAudioFilePath.")
                if (recordingForUploadFlow) {
                    recordDialog?.startProcessing()
                    processRecordedAudioAndUpload(currentAudioFilePath)
                } else {
                    aiDialog?.startProcessing()
                    processRecordedAudioForAiResponse(currentAudioFilePath)
                }
            } else {
                Log.w(TAG, "No valid audio file path or file does not exist after stopping. Path: $currentAudioFilePath")
                if (recordingForUploadFlow) recordDialog?.dismiss() else aiDialog?.dismiss()
                if (isAdded && context != null) Toast.makeText(context, "녹음된 파일이 없거나 유효하지 않습니다.", Toast.LENGTH_SHORT).show()
                cleanupRecordedFile(currentAudioFilePath) // Clean up path variable even if file didn't exist
            }
        }
    }

    // For Upload Flow (used by recordBtn via RecordDialog)
    private fun processRecordedAudioAndUpload(filePath: String?) {
        val safeContext = context ?: run {
            Log.e(TAG, "Context is null for upload.")
            recordDialog?.dismiss()
            cleanupRecordedFile(filePath)
            return
        }
        // ... (rest of the upload logic from previous correct version)
        if (filePath == null) { /* ... */ return }
        val audioFile = File(filePath)
        if (!audioFile.exists() || audioFile.length() == 0L) { /* ... */ cleanupRecordedFile(filePath); return }
        val currentMeetingId = meetingId ?: run { /* ... */ cleanupRecordedFile(filePath); return }
        val topic = meetingDetail?.title?.trim() ?: "녹음된 회의"

        Log.d(TAG, "Uploading recorded audio. Topic: '$topic', Meeting ID: $currentMeetingId, File: $filePath")
        uploadJob?.cancel()
        uploadJob = viewLifecycleOwner.lifecycleScope.launch {
            var uploadSuccess = false
            var apiMessage: String? = null
            try {
                val requestFile = audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
                val isUserSubscribed = PreferencesUtil(safeContext).isSubscribed()

                val response : Response<out Any> = withContext(Dispatchers.IO) {
                    if (isUserSubscribed) {
                        apiService.uploadAudioSummaryClova(topic, currentMeetingId, filePart)
                    } else {
                        apiService.uploadAudioSummary(topic, currentMeetingId, filePart)
                    }
                }
                if (response.isSuccessful) {
                    uploadSuccess = true
                    apiMessage = "녹음 파일 업로드 및 처리 성공!"
                    Log.d(TAG, "Recorded audio upload successful. Code: ${response.code()}")
                    loadMeetingDetails(currentMeetingId)
                } else {
                    val errorBody = (response as? Response<ResponseBody>)?.errorBody()?.string() ?: response.message() ?: "알 수 없는 오류"
                    Log.e(TAG, "Recorded audio upload failed. Code: ${response.code()}, Message: $errorBody")
                    apiMessage = "업로드 실패 (코드: ${response.code()}): $errorBody"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during recorded audio upload", e)
                apiMessage = if (e is kotlinx.coroutines.CancellationException) "업로드가 취소되었습니다." else "오류 발생: ${e.message}"
            } finally {
                withContext(Dispatchers.Main) {
                    recordDialog?.dismiss()
                    if (isAdded) Toast.makeText(safeContext, apiMessage, Toast.LENGTH_LONG).show()
                }
                cleanupRecordedFile(filePath) // Ensure original recording is cleaned up
            }
        }
    }

    // For AI Voice Response Flow (used by floating aiCardView via AiDialog)
    private fun processRecordedAudioForAiResponse(filePath: String?) {
        val safeContext = context ?: run {
            Log.e(TAG, "Context is null for AI response.")
            aiDialog?.dismiss()
            cleanupRecordedFile(filePath)
            return
        }
        if (filePath == null) {
            Log.e(TAG, "Audio file path is null for AI response.")
            Toast.makeText(safeContext, "녹음 파일 경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            aiDialog?.dismiss()
            return
        }
        val audioFile = File(filePath)
        if (!audioFile.exists() || audioFile.length() == 0L) {
            Log.e(TAG, "Audio file does not exist or is empty for AI response: $filePath")
            Toast.makeText(safeContext, "녹음된 파일이 없거나 비어있습니다.", Toast.LENGTH_SHORT).show()
            aiDialog?.dismiss()
            cleanupRecordedFile(filePath)
            return
        }

        Log.d(TAG, "Processing recorded audio for AI response. File: $filePath")
        lifecycleScope.launch {
            var apiMessage: String? = null
            var success = false
            try {
                val requestFile = audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)

                val response = withContext(Dispatchers.IO) {
                    apiService.getAiReply(body) // Assumes getAiReply returns Response<ResponseBody> with audio
                }

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val audioBytes = responseBody.bytes()
                        saveAndAutoPlayAudioResponse(audioBytes) // This will handle aiDialog.startAiResponse
                        success = true // Assuming saveAndAutoPlay handles its own UI updates via aiDialog
                    } else {
                        apiMessage = "AI 응답 내용이 없습니다."
                        aiDialog?.dismiss()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: response.message() ?: "알 수 없는 오류"
                    Log.e(TAG, "AI reply failed. Code: ${response.code()}, Message: $errorBody")
                    apiMessage = "AI 응답 실패 (코드: ${response.code()}): $errorBody"
                    aiDialog?.dismiss()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception processing audio for AI response", e)
                apiMessage = if (e is kotlinx.coroutines.CancellationException) "AI 응답 처리가 취소되었습니다." else "AI 응답 처리 중 오류: ${e.message}"
                aiDialog?.dismiss()
            } finally {
                if (!success && apiMessage != null && isAdded) { // Only show toast if saveAndAutoPlay didn't take over UI
                    Toast.makeText(safeContext, apiMessage, Toast.LENGTH_LONG).show()
                }
                cleanupRecordedFile(filePath) // Ensure original recording is cleaned up
            }
        }
    }

    // --- AI Voice Response Playback Helper Functions (Restored) ---
    private fun saveAndAutoPlayAudioResponse(audioBytes: ByteArray) {
        if (!isAdded || context == null) return
        try {
            val responseDir = File(requireActivity().externalCacheDir, "ai_responses")
            if (!responseDir.exists()) responseDir.mkdirs()
            currentResponseAudioPath = "${responseDir.absolutePath}/ai_response_${System.currentTimeMillis()}.mp3" // Or m4a
            val responseFile = File(currentResponseAudioPath!!)
            responseFile.writeBytes(audioBytes)
            autoPlayAudioResponse(currentResponseAudioPath!!)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving AI response audio", e)
            aiDialog?.dismiss() // Dismiss if saving fails
            Toast.makeText(context, "AI 응답 저장 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun autoPlayAudioResponse(audioPath: String) {
        if (!isAdded || context == null) return
        stopAudioResponse() // Stop any previous playback
        responseMediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(audioPath)
                prepareAsync()
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.start()
                    aiDialog?.startAiResponse(mediaPlayer) // Notify AiDialog to change state
                }
                setOnCompletionListener {
                    aiDialog?.stopAiResponse()
                    cleanupResponseAudio() // Clean up the AI's response file
                    // Optional: auto-dismiss AiDialog after a delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (aiDialog?.isDialogShowing == true) { // Check if still showing
                            aiDialog?.dismiss()
                        }
                    }, 500)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what $what, extra $extra")
                    aiDialog?.stopAiResponse()
                    aiDialog?.dismiss()
                    cleanupResponseAudio()
                    Toast.makeText(context, "AI 응답 재생 실패", Toast.LENGTH_SHORT).show()
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "MediaPlayer setup failed for AI response", e)
                aiDialog?.stopAiResponse()
                aiDialog?.dismiss()
                cleanupResponseAudio()
                Toast.makeText(context, "AI 응답 재생 준비 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopAudioResponse() {
        responseMediaPlayer?.apply {
            try {
                if (isPlaying) stop()
                reset()
                release()
            } catch (e: Exception) { Log.e(TAG, "Error stopping/releasing responseMediaPlayer", e) }
        }
        responseMediaPlayer = null
        aiDialog?.stopAiResponse() // Ensure AiDialog state is reset
    }

    private fun cleanupResponseAudio() { // Cleans up the AI's response audio file
        currentResponseAudioPath?.let { path ->
            try {
                val file = File(path)
                if (file.exists() && file.delete()) Log.d(TAG, "Deleted AI response audio file: $path")
            } catch (e: Exception) { Log.e(TAG, "Error deleting AI response audio file", e) }
        }
        currentResponseAudioPath = null
    }
    // --- End of AI Voice Response Playback Helpers ---

    private fun cleanupRecordedFile(filePath: String?) { // Cleans up the user's original recording
        filePath?.let {
            try {
                val file = File(it)
                if (file.exists() && file.delete()) Log.d(TAG, "Deleted recorded file: $it")
                else if (file.exists()) Log.w(TAG, "Failed to delete recorded file: $it")
            } catch (e: Exception) { Log.e(TAG, "Error deleting recorded file: $it", e) }
        }
        if (this.audioFilePath == filePath) { // Only nullify if it's the current main path
            this.audioFilePath = null
        }
    }

    private fun checkRecordingPermission(): Boolean = if(context != null) ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED else false
    private fun requestRecordingPermission() { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION) }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!isRecording) {
                    // Show the correct dialog based on which flow initiated permission request
                    // This part is tricky if requestRecordingPermission() is generic.
                    // For now, assume the flag 'recordingForUploadFlow' was set *before* permission was requested.
                    if (recordingForUploadFlow) {
                        recordDialog?.show()
                    } else {
                        aiDialog?.show()
                    }
                    startRecording()
                }
            } else {
                if (isAdded && context != null) Toast.makeText(context, "녹음 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showInitialView() {
        if (!isAdded || context == null) return
        Log.d(TAG, "Displaying initial view")
        _binding?.apply {
            meetTitle.text = "회의 정보 로드 실패"
            meetDate.text = "" ; meetTime.text = "" ; meetPlace.text = ""
            profileImg.visibility = View.GONE
            while (participantsLayout.childCount > 1) {
                participantsLayout.removeViewAt(participantsLayout.childCount - 1)
            }

            if (isSubscribed) {
                tabLayout.visibility = View.VISIBLE
                summerizedText.visibility = View.GONE
                updateUiForTabSelection(currentTabPosition, false, false)
            } else {
                tabLayout.visibility = View.GONE
                summerizedText.visibility = View.VISIBLE
                summerizedText.text = "회의 정보를 불러오지 못했습니다.\n새로운 회의를 만들거나 기존 회의를 선택해주세요."
                recordBtn.visibility = View.VISIBLE
                uploadBtn.visibility = View.VISIBLE
                textViewMinutesPlaceholder.visibility = View.GONE
                defaultContentLayout.visibility = View.GONE
                summaryScrollView.visibility = View.GONE
                transcriptScrollView.visibility = View.GONE
            }
        }
    }

    private fun showSummaryView(summaryText: String) { // Primarily for UploadBottomSheet callback
        if (!isAdded || _binding == null || context == null) return
        requireActivity().runOnUiThread {
            _binding?.apply {
                if (isSubscribed) { // Subscribers use tabs, so loadMeetingDetails is better
                    // This might be called after UploadBottomSheet, so we ensure meeting details are reloaded
                    // which then updates the tabs.
                    // For simplicity, if this is directly setting summary, ensure it's on the correct tab.
                    // However, relying on loadMeetingDetails is cleaner.
                    // summaryTextView.text = summaryText
                    // updateUiForTabSelection(0, !summaryText.isBlank(), hasTranscript)
                    // The above is now handled by loadMeetingDetails called from UploadBottomSheet callback.
                } else { // Non-subscribers use summerizedText
                    textViewMinutesPlaceholder.visibility = View.GONE
                    recordBtn.visibility = View.GONE
                    uploadBtn.visibility = View.GONE
                    summerizedText.visibility = View.VISIBLE
                    summerizedText.text = summaryText
                }
            }
        }
    }


    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop called. isRecording: $isRecording")
        if (isRecording) {
            stopRecording() // This will correctly dismiss the appropriate dialog
        }
        uploadJob?.cancel() // Cancel any upload from recordBtn flow
        stopAudioResponse() // Stop AI voice response playback
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called.")
        mediaRecorder?.release()
        mediaRecorder = null

        cleanupRecordedFile(audioFilePath) // Clean up user's recording
        uploadJob?.cancel()

        stopAudioResponse() // Stop and release AI response MediaPlayer
        cleanupResponseAudio() // Clean up AI's response audio file

        aiDialog?.dismiss()
        aiDialog = null
        recordDialog?.dismiss()
        recordDialog = null
        _binding = null
    }

    companion object {
        const val TAG = "MeetDetailFragment"
        const val REQUEST_RECORD_AUDIO_PERMISSION = 200
        @JvmStatic
        fun newInstance(meetingId: Long): MeetDetailFragment =
            MeetDetailFragment().apply { arguments = Bundle().apply { putLong(ARG_MEETING_ID, meetingId) } }
    }
}