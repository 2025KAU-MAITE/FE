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
import android.widget.Toast
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

    // 음성 응답 재생용 MediaPlayer
    private var responseMediaPlayer: MediaPlayer? = null
    private var currentResponseAudioPath: String? = null

    // API 서비스
    private lateinit var apiService: MaiteApiService

    // 구독 상태
    private var isSubscribed = false

    // 현재 선택된 탭 (0: 요약본, 1: 회의록)
    private var currentTabPosition = 0

    // 회의 콘텐츠 상태 저장 변수
    private var hasSummary = false
    private var hasTranscript = false
    private var summaryContent: String? = null
    private var transcriptContent: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_MEETING_ID)) {
                meetingId = it.getLong(ARG_MEETING_ID)
                Log.d(TAG, "MeetDetailFragment 초기화: meetingId=$meetingId")
            }
        }

        childFragmentManager.setFragmentResultListener(UploadBottomSheet.REQUEST_KEY_UPLOAD, this) { _, bundle ->
            val success = bundle.getBoolean(UploadBottomSheet.BUNDLE_KEY_SUCCESS)
            if (success) {
                Log.d(TAG, "Upload 성공 결과 수신")
                // UI 업데이트는 메인 스레드에서 실행되도록 보장
                requireActivity().runOnUiThread {
                    // 회의 상세 정보를 다시 로드하여 최신 데이터를 가져옴
                    meetingId?.let { id ->
                        Toast.makeText(requireContext(), "요약 생성 중입니다...", Toast.LENGTH_SHORT).show()
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

        // 구독 상태 확인
        val preferencesUtil = PreferencesUtil(requireContext())
        isSubscribed = preferencesUtil.isSubscribed()
        Log.d(TAG, "구독 상태: $isSubscribed")

        aiDialog = AiDialog(requireContext()).apply {
            onAiCardViewClick = {
                if (isRecording) {
                    // 녹음 중지하고 로딩 상태로 전환 (dialog는 유지)
                    stopRecording()
                    this.startProcessing() // 로딩 애니메이션 시작
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

            // 구독 상태에 따라 UI 초기화
            if (isSubscribed) {
                // 구독자는 탭 레이아웃 표시
                setupSubscribedUI()
            } else {
                // 비구독자는 기본 UI 표시
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

            // uploadBtn 클릭 리스너 수정 - meetingId를 함께 전달
            b.uploadBtn.setOnClickListener {
                val currentTitle = meetingDetail?.title ?: b.meetTitle.text.toString()
                val currentMeetingId = meetingId

                if (currentTitle.isBlank() && meetingDetail == null) {
                    Toast.makeText(requireContext(), "회의 제목을 불러오는 중입니다...", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (currentMeetingId == null) {
                    Toast.makeText(requireContext(), "회의 정보를 불러오는 중입니다...", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "uploadBtn 클릭 시 meetingId가 null")
                    return@setOnClickListener
                }

                Log.d(TAG, "UploadBottomSheet 호출: title='$currentTitle', meetingId=$currentMeetingId")

                // meetingId를 함께 전달하는 새로운 팩토리 메서드 사용
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
        } ?: Log.e(TAG, "Binding is null in onViewCreated.")
    }

    // 구독자용 UI 설정
    private fun setupSubscribedUI() {
        binding.apply {
            // 무료 사용자 UI 유지 - 스크린샷처럼 기본 UI를 그대로 두고 탭만 추가
            textViewMinutesPlaceholder.visibility = View.VISIBLE
            recordBtn.visibility = View.VISIBLE
            uploadBtn.visibility = View.VISIBLE

            // 요약 텍스트 숨기기
            summerizedText.text = ""
            summerizedText.visibility = View.GONE

            // 탭 레이아웃 표시
            tabLayout.visibility = View.VISIBLE

            // 첫 번째 탭 선택 및 배경색 설정
            val firstTab = tabLayout.getTabAt(0)
            firstTab?.select()
            firstTab?.view?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.subColor))

            // 탭 선택 리스너 설정
            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.let {
                        currentTabPosition = it.position
                        // 저장된 콘텐츠 상태를 전달하여 UI 업데이트
                        updateUiForTabSelection(currentTabPosition, hasSummary, hasTranscript)
                        // 탭 배경색 설정
                        tab.view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.subColor))
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    tab?.view?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })

            // 회의 상세 정보 로드는 외부에서 이미 호출됨
        }
    }

    // 탭 선택에 따른 UI 업데이트
    private fun updateUiForTabSelection(position: Int, hasValidSummary: Boolean = false, hasValidTranscript: Boolean = false) {
        binding.apply {
            // 요약 텍스트는 항상 숨김 및 초기화
            summerizedText.text = ""
            summerizedText.visibility = View.GONE

            // 녹음 및 업로드 버튼은 콘텐츠 유무에 따라 표시 여부 결정
            val hasContent = when (position) {
                0 -> hasValidSummary  // 요약본 탭에서는 요약본 유무에 따라 결정
                1 -> hasValidTranscript    // 회의록 탭에서는 회의록 유무에 따라 결정
                else -> false
            }

            // 콘텐츠가 있으면 버튼 숨김, 없으면 표시
            recordBtn.visibility = if (hasContent) View.GONE else View.VISIBLE
            uploadBtn.visibility = if (hasContent) View.GONE else View.VISIBLE

            when (position) {
                0 -> { // 요약본 탭
                    // 회의록 영역은 항상 숨김
                    transcriptScrollView.visibility = View.GONE

                    if (hasValidSummary) {
                        // 요약 내용이 있으면 표시
                        summaryScrollView.visibility = View.VISIBLE
                        textViewMinutesPlaceholder.visibility = View.GONE
                    } else {
                        // 요약 내용이 없으면 안내 메시지 표시
                        summaryScrollView.visibility = View.GONE
                        textViewMinutesPlaceholder.text = "등록된 요약본이 없어요"
                        textViewMinutesPlaceholder.visibility = View.VISIBLE
                    }
                }
                1 -> { // 회의록 탭
                    // 요약 영역은 항상 숨김
                    summaryScrollView.visibility = View.GONE

                    if (hasValidTranscript) {
                        // 회의록 내용이 있으면 표시
                        transcriptScrollView.visibility = View.VISIBLE
                        textViewMinutesPlaceholder.visibility = View.GONE
                    } else {
                        // 회의록 내용이 없으면 안내 메시지 표시
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
                Log.d(TAG, "회의 상세 정보 로드 시작: meetingId=$id")
                val response = withContext(Dispatchers.IO) {
                    apiService.getMeetingDetail(id)
                }
                if (response.isSuccessful) {
                    meetingDetail = response.body()
                    meetingDetail?.let { detail ->
                        Log.d(TAG, "회의 상세 정보 로드 성공: ${detail.title}")

                        // 요약본과 회의록이 있는지 확인
                        hasSummary = !detail.textSum.isNullOrBlank()
                        hasTranscript = !detail.recordText.isNullOrBlank()

                        // 내용 저장 (탭 전환 시 복원을 위해)
                        summaryContent = detail.textSum
                        transcriptContent = detail.recordText

                        // 구독 상태에 따라 다른 UI 업데이트
                        if (isSubscribed) {
                            binding.apply {
                                // 요약본과 회의록 설정
                                summaryTextView.text = summaryContent ?: "요약 내용이 없습니다."
                                transcriptTextView.text = transcriptContent ?: "회의록 내용이 없습니다."

                                // 현재 선택된 탭에 맞는 뷰 표시
                                updateUiForTabSelection(currentTabPosition, hasSummary, hasTranscript)
                            }
                        } else {
                            // 비구독자 UI 업데이트
                            updateUiWithMeetingDetails(detail)
                        }
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

    // 일반 API 응답 처리
    private fun handleStandardApiResponse(response: Response<ResponseBody>) {
        if (response.isSuccessful) {
            try {
                val responseBody = response.body()?.string() ?: "응답 내용이 없습니다."
                Log.d(TAG, "기본 API 응답: $responseBody")

                // 사용자에게 처리 중임을 알림
                Toast.makeText(requireContext(), "요약 생성 중입니다...", Toast.LENGTH_SHORT).show()

                // API 호출 성공 후 회의 상세 정보를 다시 로드하여 최신 데이터를 가져옴
                meetingId?.let { id ->
                    loadMeetingDetails(id)
                }

            } catch (e: Exception) {
                Log.e(TAG, "응답 처리 중 오류", e)
                Toast.makeText(requireContext(), "응답 처리 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e(TAG, "API 호출 실패: ${response.code()}")
            Toast.makeText(requireContext(), "API 호출 실패: ${response.message()}", Toast.LENGTH_SHORT).show()
        }
    }

    // Clova API 응답 처리
    private fun handleClovaApiResponse(response: Response<ClovaSummaryResponse>) {
        if (!response.isSuccessful) {
            Log.e(TAG, "클로바 API 호출 실패: ${response.code()}")
            showApiErrorMessage("API 호출 실패: ${response.message()}")
        }
    }

    private fun showApiErrorMessage(message: String) {
        binding.apply {
            summaryTextView.text = "요약본을 불러올 수 없습니다. $message"
            transcriptTextView.text = "회의록을 불러올 수 없습니다. $message"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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
                        Log.d(TAG, "Profile image URL is null or blank for $email (or user not found), using default.")
                    }
                } else {
                    imageView.setImageResource(R.drawable.img_profile_default)
                    Log.e(TAG, "Failed to fetch user details for $email or API error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
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
            Toast.makeText(requireContext(), "녹음이 완료되었습니다.", Toast.LENGTH_SHORT).show()
            updateRecordingUI(false)

            // 녹음 완료 후 즉시 AI API로 전송
            processRecordedAudio(audioFilePath)
        } catch (e: Exception) {
            Log.e(TAG, "녹음 중지 실패: ${e.message}")
        }
    }

    private fun updateRecordingUI(isRecording: Boolean) {
        // UI 업데이트 (예: 버튼 아이콘 변경 등)
    }

    // 녹음 완료 후 AI API로 파일 전송하는 메서드
    private fun processRecordedAudio(filePath: String?) {
        filePath?.let { path ->
            Log.d(TAG, "녹음 파일 처리 시작: $path")

            lifecycleScope.launch {
                try {
                    // 로딩 표시 (이미 startProcessing()으로 시작됨)

                    val file = File(path)
                    if (!file.exists()) {
                        Log.e(TAG, "녹음 파일이 존재하지 않습니다: $path")
                        Toast.makeText(requireContext(), "녹음 파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        aiDialog?.dismiss() // 에러 시 dialog 닫기
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
                            // 바이너리 데이터를 음성 파일로 저장
                            val audioBytes = responseBody.bytes()
                            Log.d(TAG, "AI 응답 수신 완료: ${audioBytes.size} bytes")

                            // 응답 음성 파일 저장 및 자동 재생
                            saveAndAutoPlayAudioResponse(audioBytes)
                        } else {
                            Toast.makeText(requireContext(), "AI 응답이 비어있습니다.", Toast.LENGTH_SHORT).show()
                            aiDialog?.dismiss()
                        }
                    } else {
                        Log.e(TAG, "AI API 호출 실패: ${response.code()} - ${response.errorBody()?.string()}")
                        Toast.makeText(requireContext(), "AI 분석에 실패했습니다: ${response.message()}", Toast.LENGTH_SHORT).show()
                        aiDialog?.dismiss()
                    }

                    // 원본 파일 정리
                    file.delete()

                } catch (e: Exception) {
                    Log.e(TAG, "AI API 호출 중 오류: ${e.message}", e)
                    Toast.makeText(requireContext(), "AI 분석 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                    aiDialog?.dismiss()
                }
            }
        }
    }

    // 로딩 상태 표시
    private fun showLoadingState() {
        _binding?.apply {
            Toast.makeText(requireContext(), "AI가 음성을 분석하고 있습니다...", Toast.LENGTH_SHORT).show()
        }
    }

    // 로딩 상태 숨김
    private fun hideLoadingState() {
        _binding?.apply {
            // 로딩 상태 해제
        }
    }

    // 음성 응답 저장 및 자동 재생
    private fun saveAndAutoPlayAudioResponse(audioBytes: ByteArray) {
        try {
            // 응답 오디오 파일 경로 생성
            val responseAudioPath = "${requireActivity().externalCacheDir?.absolutePath}/ai_response_${System.currentTimeMillis()}.mp3"
            val responseFile = File(responseAudioPath)

            // 바이너리 데이터를 파일로 저장
            responseFile.writeBytes(audioBytes)

            Log.d(TAG, "AI 응답 음성 파일 저장 완료: $responseAudioPath")
            currentResponseAudioPath = responseAudioPath

            // 자동으로 음성 재생 시작
            autoPlayAudioResponse(responseAudioPath)

        } catch (e: Exception) {
            Log.e(TAG, "음성 응답 저장 실패: ${e.message}", e)
            Toast.makeText(requireContext(), "음성 응답 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 자동 음성 재생
    private fun autoPlayAudioResponse(audioPath: String) {
        try {
            // 기존 재생 중인 것이 있다면 정지
            stopAudioResponse()

            responseMediaPlayer = MediaPlayer().apply {
                setDataSource(audioPath)
                prepareAsync()
                setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.start()
                    Log.d(TAG, "AI 응답 음성 자동 재생 시작")
                    Toast.makeText(requireContext(), "🔊 AI 응답", Toast.LENGTH_SHORT).show()

                    // AI 응답 시작을 AiDialog에 알림
                    aiDialog?.startAiResponse(mediaPlayer)

                    // 재생 완료 리스너
                    setOnCompletionListener {
                        Log.d(TAG, "AI 응답 음성 재생 완료")

                        // AI 응답 완료를 AiDialog에 알림
                        aiDialog?.stopAiResponse()

                        // 재생 완료 후 파일 정리 및 dialog 닫기
                        cleanupResponseAudio()

                        // 조금 지연 후 dialog 닫기
                        Handler(Looper.getMainLooper()).postDelayed({
                            aiDialog?.dismiss()
                        }, 500)
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "음성 재생 오류: what=$what, extra=$extra")
                    Toast.makeText(requireContext(), "음성 재생에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    aiDialog?.stopAiResponse()
                    aiDialog?.dismiss()
                    true
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "음성 재생 실패: ${e.message}", e)
            Toast.makeText(requireContext(), "음성 재생에 실패했습니다.", Toast.LENGTH_SHORT).show()
            aiDialog?.dismiss()
        }
    }

    // 음성 재생 정지
    private fun stopAudioResponse() {
        responseMediaPlayer?.apply {
            try {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            } catch (e: Exception) {
                Log.e(TAG, "MediaPlayer 정리 중 오류: ${e.message}")
            }
        }
        responseMediaPlayer = null

        // AI 응답 중지를 AiDialog에 알림
        aiDialog?.stopAiResponse()
    }

    // 응답 음성 파일 정리
    private fun cleanupResponseAudio() {
        currentResponseAudioPath?.let { path ->
            try {
                File(path).delete()
                Log.d(TAG, "응답 음성 파일 삭제 완료: $path")
            } catch (e: Exception) {
                Log.e(TAG, "응답 음성 파일 삭제 실패: ${e.message}")
            }
        }
        currentResponseAudioPath = null
    }

    // 구독자용 업로드 메서드 (Clova API 사용)
    private fun uploadAudioFileForSubscriber(filePath: String, meetingId: Long, topic: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // 파일을 MultipartBody.Part로 변환
                val requestFile = file.asRequestBody("audio/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

                // Clova API 호출
                val response = apiService.uploadAudioSummaryClova(topic, meetingId, filePart)

                withContext(Dispatchers.Main) {
                    // 응답 처리
                    if (response.isSuccessful) {
                        val clovaResponse = response.body()
                        if (clovaResponse != null && clovaResponse.isSuccess) {
                            Toast.makeText(requireContext(), "요약 생성 중입니다...", Toast.LENGTH_SHORT).show()

                            // API 호출 성공 후 회의 상세 정보를 다시 로드하여 최신 데이터를 가져옴
                            loadMeetingDetails(meetingId)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "API 처리 중 오류: ${clovaResponse?.message ?: "응답이 없습니다"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        handleClovaApiResponse(response)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "파일 업로드 중 오류 발생", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "음성 파일 업로드 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
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

        // 음성 재생 정지
        stopAudioResponse()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaRecorder?.release()
        mediaRecorder = null

        // 응답 음성 재생기 정리
        stopAudioResponse()

        // 임시 음성 파일들 정리
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