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
import androidx.lifecycle.lifecycleScope
import com.example.maite.databinding.FragmentMeetDetailBinding
import com.example.maite.model.ClovaSummaryResponse
import com.example.maite.model.MeetListItem
import com.google.android.material.tabs.TabLayout
import java.io.IOException
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.create
import okhttp3.ResponseBody
import retrofit2.Response

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
    
    // API 서비스
    private lateinit var apiService: MaiteApiService
    
    // 구독 상태
    private var isSubscribed = false
    
    // 현재 선택된 탭 (0: 요약본, 1: 회의록)
    private var currentTabPosition = 0

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
        
        // API 서비스 초기화
        apiService = MaiteRetrofitClient.getInstance(requireContext())
        
        return _binding!!.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 구독 상태 확인
        val preferencesUtil = PreferencesUtil(requireContext())
        isSubscribed = preferencesUtil.isSubscribed()
        Log.d(TAG, "구독 상태: $isSubscribed")

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
                val meetingId = meetItem?.meetingId ?: -1L
                val uploadBottomSheet = UploadBottomSheet.newInstance(currentTitle, meetingId)
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

    // 구독자용 UI 설정
    private fun setupSubscribedUI() {
        binding?.apply {
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
                        updateUiForTabSelection(currentTabPosition)
                        // 탭 배경색 설정
                        tab.view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.subColor))
                    }
                }
                
                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    tab?.view?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
                
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
            
            // 초기 탭 상태에 따라 UI 업데이트
            updateUiForTabSelection(currentTabPosition)
            
            // 회의 상세 정보 로드
            loadMeetingDetail()
        }
    }
    
    // 일반 API 응답 처리
    private fun handleStandardApiResponse(response: Response<ResponseBody>) {
        if (response.isSuccessful) {
            try {
                val responseBody = response.body()?.string() ?: "응답 내용이 없습니다."
                Log.d(TAG, "기본 API 응답: $responseBody")
                
                // 만약 일반 사용자 UI를 사용 중이라면 요약 뷰로 전환
                showSummaryView(responseBody)
                
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
        if (response.isSuccessful) {
            val clovaResponse = response.body()
            if (clovaResponse != null && clovaResponse.isSuccess) {
                Log.d(TAG, "클로바 API 응답 성공: ${clovaResponse.result}")
                
                // 요약 내용이 있는지 확인 (요약할 내용이 없다는 메시지가 아닌지)
                val hasValidSummary = clovaResponse.result?.result?.contains("요약할 내용이 없습니다") == false
                val hasTranscript = !clovaResponse.result?.transcript.isNullOrBlank()
                
                binding?.apply {
                    if (hasValidSummary) {
                        // 실제 요약 내용이 있을 때
                        summaryTextView.text = clovaResponse.result?.result
                        summaryScrollView.visibility = View.VISIBLE
                    } else {
                        // 요약할 내용이 없을 때
                        summaryTextView.text = ""
                        summaryScrollView.visibility = View.GONE
                    }
                    
                    if (hasTranscript) {
                        // 실제 회의록 내용이 있을 때
                        transcriptTextView.text = clovaResponse.result?.transcript
                        transcriptScrollView.visibility = View.VISIBLE
                    } else {
                        // 회의록 내용이 없을 때
                        transcriptTextView.text = ""
                        transcriptScrollView.visibility = View.GONE
                    }
                    
                    // 현재 선택된 탭에 맞는 뷰 표시 - 내용이 있는지 여부 전달
                    updateUiForTabSelection(currentTabPosition, hasValidSummary, hasTranscript)
                }
                
            } else {
                Log.e(TAG, "클로바 API 응답 내용 없음 또는 실패: ${clovaResponse?.message}")
                showApiErrorMessage("응답 오류: ${clovaResponse?.message ?: "내용 없음"}")
            }
        } else {
            Log.e(TAG, "클로바 API 호출 실패: ${response.code()}")
            showApiErrorMessage("API 호출 실패: ${response.message()}")
        }
    }
    
    // 탭 선택에 따른 UI 업데이트
    private fun updateUiForTabSelection(position: Int, hasValidSummary: Boolean = false, hasTranscript: Boolean = false) {
        binding?.apply {
            // 요약 텍스트는 항상 숨김 및 초기화
            summerizedText.text = ""
            summerizedText.visibility = View.GONE
            
            // 녹음 및 업로드 버튼은 항상 표시
            recordBtn.visibility = View.VISIBLE
            uploadBtn.visibility = View.VISIBLE
            
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
                    
                    if (hasTranscript) {
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
    
    // 회의 상세 정보 API 호출
    private fun loadMeetingDetail() {
        // meetItem에서 필요한 정보 추출
        val meetingId = meetItem?.meetingId ?: 1L
        
        lifecycleScope.launch {
            try {
                // 로딩 표시 
                binding?.apply {
                    summaryTextView.text = "요약본을 불러오는 중..."
                    transcriptTextView.text = "회의록을 불러오는 중..."
                }
                
                // 회의 상세 API 호출
                val response = withContext(Dispatchers.IO) {
                    apiService.getMeetingDetail(meetingId)
                }
                
                if (response.isSuccessful) {
                    val meetingDetail = response.body()
                    if (meetingDetail != null) {
                        // record 필드 확인하여 내용이 있는지 검사
                        val hasRecord = !meetingDetail.record.isNullOrBlank()
                        val hasValidSummary = !meetingDetail.textSum.isNullOrBlank()
                        val hasTranscript = !meetingDetail.recordText.isNullOrBlank()
                        
                        binding?.apply {
                            // 요약본과 회의록 설정
                            summaryTextView.text = meetingDetail.textSum ?: "요약 내용이 없습니다."
                            transcriptTextView.text = meetingDetail.recordText ?: "회의록 내용이 없습니다."
                            
                            // 현재 선택된 탭에 맞는 뷰 표시
                            updateUiForTabSelection(currentTabPosition, hasValidSummary, hasTranscript)
                        }
                        Log.d(TAG, "회의 상세 API 호출 성공: record=${hasRecord}, textSum=${hasValidSummary}, recordText=${hasTranscript}")
                    } else {
                        Log.e(TAG, "회의 상세 API 응답 내용 없음")
                        showApiErrorMessage("응답 내용이 없습니다.")
                    }
                } else {
                    Log.e(TAG, "회의 상세 API 호출 실패: ${response.code()}")
                    showApiErrorMessage("API 호출 실패: ${response.message()}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "회의 상세 API 호출 중 오류", e)
                showApiErrorMessage("오류: ${e.message}")
            }
        }
    }
    
    // Clova API 호출하여 회의록 및 요약본 가져오기
    private fun loadClovaMeetingData() {
        // meetItem에서 필요한 정보 추출
        val meetingId = meetItem?.meetingId ?: 1L
        val topic = meetItem?.title ?: "회의"
        
        lifecycleScope.launch {
            try {
                // 로딩 표시 
                binding?.apply {
                    summaryTextView.text = "요약본을 불러오는 중..."
                    transcriptTextView.text = "회의록을 불러오는 중..."
                }
                
                // 빈 파일 생성하여 API 요청 (Clova API는 파일이 필요함)
                val emptyFile = File(requireContext().cacheDir, "empty_request_${System.currentTimeMillis()}.txt")
                emptyFile.createNewFile()
                
                val requestFile = emptyFile.asRequestBody("text/plain".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", emptyFile.name, requestFile)
                
                // API 호출
                val response = withContext(Dispatchers.IO) {
                    apiService.uploadAudioSummaryClova(topic, meetingId, filePart)
                }
                
                // 임시 파일 삭제
                emptyFile.delete()
                
                if (response.isSuccessful) {
                    val clovaResponse = response.body()
                    if (clovaResponse != null && clovaResponse.isSuccess && clovaResponse.result != null) {
                        // 요약 내용이 있는지 확인 (요약할 내용이 없다는 메시지가 아닌지)
                        val result = clovaResponse.result
                        val hasValidSummary = result.result?.contains("요약할 내용이 없습니다") == false
                        val hasTranscript = !result.transcript.isNullOrBlank()
                        
                        binding?.apply {
                            summaryTextView.text = result.result ?: "요약 내용이 없습니다."
                            transcriptTextView.text = result.transcript ?: "회의록 내용이 없습니다."
                            
                            // 현재 선택된 탭에 맞는 뷰 표시
                            updateUiForTabSelection(currentTabPosition, hasValidSummary, hasTranscript)
                        }
                        Log.d(TAG, "클로바 API 호출 성공: $result")
                    } else {
                        Log.e(TAG, "클로바 API 응답 내용 없음")
                        showApiErrorMessage("응답 내용이 없습니다.")
                    }
                } else {
                    Log.e(TAG, "클로바 API 호출 실패: ${response.code()}")
                    showApiErrorMessage("API 호출 실패: ${response.message()}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "클로바 API 호출 중 오류", e)
                showApiErrorMessage("오류: ${e.message}")
            }
        }
    }
    
    private fun showApiErrorMessage(message: String) {
        binding?.apply {
            summaryTextView.text = "요약본을 불러올 수 없습니다. $message"
            transcriptTextView.text = "회의록을 불러올 수 없습니다. $message"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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
            
            // 녹음 파일을 API에 업로드
            uploadAudioFile(it)
        }
    }
    
    private fun uploadAudioFile(filePath: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                // 제목 가져오기
                val topic = binding?.meetTitle?.text?.toString() ?: "무제"
                
                // 파일을 MultipartBody.Part로 변환
                val requestFile = file.asRequestBody("audio/*".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
                
                // 구독 상태에 따라 다른 API 사용
                val response = if (isSubscribed) {
                    // 프리미엄 사용자는 Clova API 사용
                    val meetingId = meetItem?.meetingId ?: -1L
                    Log.d(TAG, "프리미엄 사용자: Clova API 호출 (topic: $topic, meetingId: $meetingId)")
                    apiService.uploadAudioSummaryClova(topic, meetingId, filePart)
                } else {
                    // 일반 사용자는 기본 API 사용
                    Log.d(TAG, "일반 사용자: 기본 API 호출 (topic: $topic)")
                    apiService.uploadAudioSummary(topic, filePart)
                }
                
                withContext(Dispatchers.Main) {
                    if (isSubscribed) {
                        // 프리미엄 사용자 응답 처리
                        @Suppress("UNCHECKED_CAST")
                        handleClovaApiResponse(response as Response<ClovaSummaryResponse>)
                        
                        // API 호출 성공 후 회의 상세 정보를 다시 로드하여 최신 데이터를 가져옴
                        loadMeetingDetail()
                    } else {
                        // 일반 사용자 응답 처리
                        @Suppress("UNCHECKED_CAST")
                        handleStandardApiResponse(response as Response<ResponseBody>)
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
            // 요약본 메시지만 표시하고 다른 텍스트 표시하지 않음
            textViewMinutesPlaceholder.text = "등록된 요약본이 없어요"
            textViewMinutesPlaceholder.visibility = View.VISIBLE
            
            // 녹음 및 업로드 버튼 표시
            recordBtn.visibility = View.VISIBLE
            uploadBtn.visibility = View.VISIBLE
            
            // 요약 텍스트는 숨김 처리 및 초기화
            summerizedText.text = ""
            summerizedText.visibility = View.GONE
            
            Log.d(TAG, "초기 뷰 상태 설정됨")
        }
    }

    // 업로드 성공 후 UI
    private fun showSummaryView(summaryText: String) {
        // requireActivity()를 사용하여 Activity의 runOnUiThread 호출
        requireActivity().runOnUiThread {
            binding?.apply {
                // 기존 UI 요소 유지 (녹음, 첨부 버튼)
                textViewMinutesPlaceholder.visibility = View.VISIBLE
                recordBtn.visibility = View.VISIBLE
                uploadBtn.visibility = View.VISIBLE
                
                // 모든 불필요한 텍스트 제거 - API에서 반환된 내용만 보여줌
                val processedText = summaryText.takeIf { 
                    it.isNotBlank() && !it.contains("회의는 아직 시작되지 않았습니다") 
                } ?: "등록된 요약본이 없어요"
                
                // 요약 텍스트가 의미 있는 내용일 때만 표시
                if (processedText != "등록된 요약본이 없어요") {
                    summerizedText.text = processedText
                    summerizedText.visibility = View.VISIBLE
                    // placeholder는 숨김
                    textViewMinutesPlaceholder.visibility = View.GONE
                } else {
                    // 의미 있는 내용이 없으면 placeholder만 표시
                    summerizedText.text = ""
                    summerizedText.visibility = View.GONE
                    textViewMinutesPlaceholder.text = processedText
                }
                
                Log.d(TAG, "요약 뷰 표시됨 (on UI thread): $processedText")
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