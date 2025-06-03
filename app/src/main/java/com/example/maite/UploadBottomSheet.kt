package com.example.maite

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.example.maite.databinding.BottomSheetUploadBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.InputStream

class UploadBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetUploadBinding? = null
    private val binding get() = _binding!!

    private lateinit var audioPickerLauncher: ActivityResultLauncher<String>
    private var selectedFileUri: Uri? = null
    private lateinit var apiService: MaiteApiService
    private var defaultTopic: String? = null
    private var meetingId: Long? = null  // 새로 추가된 meetingId 변수
    private lateinit var loadingDialog: LoadingDialog
    private var uploadJob: Job? = null

    private var originalDimAmount: Float = 0.6f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            defaultTopic = it.getString(ARG_DEFAULT_TOPIC)
            meetingId = it.getLong(ARG_MEETING_ID, -1L)  // meetingId 가져오기

            if (meetingId == -1L) meetingId = null  // 기본값인 경우 null로 설정

            Log.d(TAG, "UploadBottomSheet 초기화: defaultTopic='$defaultTopic', meetingId=$meetingId")
        }

        loadingDialog = LoadingDialog(requireContext())

        audioPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedFileUri = it
                updateFileName(it)
                updateDoneButtonState()
            } ?: run {
                selectedFileUri = null
                binding.file.text = "파일을 선택해주세요"
                context?.let { ctx ->
                    binding.file.setTextColor(ContextCompat.getColor(ctx, R.color.gray))
                    Toast.makeText(ctx, "파일이 선택되지 않았습니다.", Toast.LENGTH_SHORT).show()
                }
                updateDoneButtonState()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetUploadBinding.inflate(inflater, container, false)
        originalDimAmount = dialog?.window?.attributes?.dimAmount ?: 0.6f
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        apiService = MaiteRetrofitClient.getInstance(requireContext())

        binding.titleEditText.hint = "주제를 입력하세요 (선택)"
        binding.fileCardView.setOnClickListener {
            if (!loadingDialog.isDialogShowing) {
                audioPickerLauncher.launch("audio/*")
            }
        }
        binding.doneBtn.setOnClickListener {
            handleUpload()
        }
        updateDoneButtonState()
        if (originalDimAmount == 0.6f) {
            originalDimAmount = dialog?.window?.attributes?.dimAmount ?: 0.6f
        }
    }

    private fun updateFileName(uri: Uri) {
        val safeContext = context ?: return
        val contentResolver = safeContext.contentResolver
        var fileName: String? = "Unknown File"
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
            binding.file.text = fileName
            binding.file.setTextColor(ContextCompat.getColor(safeContext, R.color.black))
            Log.d(TAG, "선택된 파일 이름: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "파일 이름 가져오기 실패", e)
            binding.file.text = "파일 이름 읽기 오류"
            binding.file.setTextColor(ContextCompat.getColor(safeContext, R.color.mainColor))
        }
    }

    private fun setUiEnabled(enabled: Boolean) {
        _binding?.let { b ->
            val isFileSelected = selectedFileUri != null
            val isDoneEnabled = enabled && isFileSelected
            b.doneBtn.isEnabled = isDoneEnabled
            b.doneBtn.isClickable = isDoneEnabled
            b.fileCardView.isEnabled = enabled
            b.fileCardView.isClickable = enabled
            b.titleEditText.isEnabled = enabled
            updateDoneButtonState()
        }
    }

    private fun handleUpload() {
        val safeContext = context
        if (safeContext == null) {
            Log.e(TAG, "handleUpload - Context가 null입니다.")
            return
        }

        val enteredTopic = binding.titleEditText.text.toString().trim()
        val currentSelectedFileUri = selectedFileUri
        val currentMeetingId = meetingId

        val finalTopic: String = if (enteredTopic.isBlank()) {
            defaultTopic?.takeIf { it.isNotBlank() } ?: ""
        } else {
            enteredTopic
        }

        if (currentSelectedFileUri == null) {
            Toast.makeText(safeContext, "파일을 선택해주세요.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "handleUpload 호출되었으나 selectedFileUri가 null임.")
            return
        }

        // meetingId 검증 추가
        if (currentMeetingId == null) {
            Toast.makeText(safeContext, "회의 정보를 불러오는 중입니다...", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "meetingId가 없어서 업로드할 수 없음")
            return
        }

        setUiEnabled(false)
        dialog?.window?.setDimAmount(0f)
        loadingDialog.show()

        Log.d(TAG, "=== 업로드 시작 ===")
        Log.d(TAG, "Topic: '$finalTopic'")
        Log.d(TAG, "Meeting ID: $currentMeetingId")
        Log.d(TAG, "File URI: $currentSelectedFileUri")

        uploadJob?.cancel()
        uploadJob = viewLifecycleOwner.lifecycleScope.launch {
            var uploadSuccess = false
            var responseMessage: String? = null
            var errorMessage: String? = null

            try {
                val filePart = createMultipartBodyPartFromUri(currentSelectedFileUri, safeContext)
                if (filePart == null) {
                    errorMessage = "파일 처리 중 오류가 발생했습니다."
                    return@launch
                }

                Log.d(TAG, "API 호출 시작: api/AI/summary")
                val response: Response<ResponseBody> = withContext(Dispatchers.IO) {
                    // 새로운 API 시그니처 사용 (meetingId 추가)
                    apiService.uploadAudioSummary(finalTopic, currentMeetingId, filePart)
                }

                if (!isActive) {
                    Log.w(TAG, "API 호출 후 코루틴 취소됨")
                    errorMessage = "업로드가 취소되었습니다."
                    return@launch
                }

                if (response.isSuccessful) {
                    uploadSuccess = true
                    responseMessage = response.body()?.string()
                    Log.d(TAG, "업로드 성공: ${response.code()}")
                    setFragmentResult(REQUEST_KEY_UPLOAD, bundleOf(
                        BUNDLE_KEY_SUCCESS to true,
                        BUNDLE_KEY_RESPONSE to responseMessage
                    ))
                } else {
                    val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
                    Log.e(TAG, "업로드 실패 상세 정보:")
                    Log.e(TAG, "- HTTP 코드: ${response.code()}")
                    Log.e(TAG, "- HTTP 메시지: ${response.message()}")
                    Log.e(TAG, "- 에러 본문: $errorBody")

                    when (response.code()) {
                        401 -> errorMessage = "인증이 만료되었습니다. 다시 로그인해주세요."
                        403 -> errorMessage = "업로드 권한이 없습니다. 관리자에게 문의하세요."
                        413 -> errorMessage = "파일 크기가 너무 큽니다."
                        415 -> errorMessage = "지원하지 않는 파일 형식입니다."
                        else -> errorMessage = "업로드 실패: ${response.message()}"
                    }
                }

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.w(TAG, "업로드 코루틴 취소됨", e)
                    errorMessage = "업로드가 취소되었습니다."
                } else {
                    Log.e(TAG, "업로드 중 예외 발생", e)
                    errorMessage = "오류 발생: ${e.message}"
                }
            } finally {
                Log.d(TAG, "API 호출 완료 (코루틴 finally)")
                if (::loadingDialog.isInitialized) {
                    loadingDialog.dismiss()
                }

                withContext(Dispatchers.Main) {
                    try {
                        dialog?.window?.setDimAmount(originalDimAmount)
                    } catch (e: Exception) {
                        Log.w(TAG, "DimAmount 복원 중 오류 발생", e)
                    }

                    if (isAdded && context != null) {
                        if (uploadSuccess) {
                            Toast.makeText(requireContext(), "파일 업로드 성공!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), errorMessage ?: "업로드 처리 중 문제가 발생했습니다.", Toast.LENGTH_LONG).show()
                        }

                        try {
                            setUiEnabled(true)
                            dismiss()
                        } catch (e: IllegalStateException) {
                            Log.e(TAG, "BottomSheet dismiss 중 오류 발생", e)
                            try {
                                dismissAllowingStateLoss()
                            } catch (ignored: Exception) {}
                        }
                    } else {
                        Log.w(TAG, "Fragment가 detached되어 UI 업데이트 및 dismiss를 건너뜁니다.")
                    }
                }
            }
        }
    }

    private suspend fun createMultipartBodyPartFromUri(uri: Uri, context: Context): MultipartBody.Part? {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                var fileName: String? = "audio_record.bin"
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val displayName = cursor.getString(nameIndex)
                            if (!displayName.isNullOrBlank()) {
                                fileName = displayName
                            }
                        }
                    }
                }
                Log.d(TAG, "Multipart 생성 - 파일 이름: $fileName, MIME 타입: $mimeType")

                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e(TAG, "파일 InputStream을 열 수 없습니다.")
                    return@withContext null
                }

                val fileBytes = inputStream.use { it.readBytes() }

                val requestBody: RequestBody = fileBytes.toRequestBody(
                    mimeType.toMediaTypeOrNull()
                )

                MultipartBody.Part.createFormData("file", fileName, requestBody)

            } catch (e: Exception) {
                Log.e(TAG, "Multipart 생성 중 오류 발생", e)
                null
            }
        }
    }

    private fun updateDoneButtonState() {
        val safeContext = context ?: return
        val currentBinding = _binding ?: return

        val isLoading = ::loadingDialog.isInitialized && loadingDialog.isDialogShowing
        val isFileSelected = selectedFileUri != null
        val isEnabled = !isLoading && isFileSelected

        currentBinding.doneBtn.isEnabled = isEnabled
        currentBinding.doneBtn.isClickable = isEnabled

        if (isEnabled) {
            currentBinding.btnBg.setColorFilter(ContextCompat.getColor(safeContext, R.color.mainColor))
            currentBinding.btnText.setTextColor(ContextCompat.getColor(safeContext, R.color.white))
        } else {
            currentBinding.btnBg.setColorFilter(ContextCompat.getColor(safeContext, R.color.btn_inactive))
            currentBinding.btnText.setTextColor(ContextCompat.getColor(safeContext, R.color.black))
        }
        Log.d(TAG, "완료 버튼 상태 업데이트: isEnabled=$isEnabled (isLoading=$isLoading, isFileSelected=$isFileSelected)")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        uploadJob?.cancel()
        uploadJob = null
        if (::loadingDialog.isInitialized && loadingDialog.isDialogShowing) {
            loadingDialog.dismiss()
            try {
                dialog?.window?.setDimAmount(originalDimAmount)
            } catch (e: Exception) {
                Log.w(TAG, "onDestroyView에서 DimAmount 복원 중 오류 발생", e)
            }
        }
        _binding = null
    }

    companion object {
        const val TAG = "UploadBottomSheet"
        private const val ARG_DEFAULT_TOPIC = "default_topic"
        private const val ARG_MEETING_ID = "meeting_id"  // 새로 추가된 상수
        const val REQUEST_KEY_UPLOAD = "uploadResultRequest"
        const val BUNDLE_KEY_SUCCESS = "uploadSuccess"
        const val BUNDLE_KEY_RESPONSE = "uploadResponse"

        // 기존 팩토리 메서드 (하위 호환성)
        fun newInstance(defaultTopic: String?): UploadBottomSheet {
            val fragment = UploadBottomSheet()
            val args = Bundle()
            args.putString(ARG_DEFAULT_TOPIC, defaultTopic)
            fragment.arguments = args
            return fragment
        }

        // meetingId를 받는 새로운 팩토리 메서드
        fun newInstance(defaultTopic: String?, meetingId: Long): UploadBottomSheet {
            val fragment = UploadBottomSheet()
            val args = Bundle()
            args.putString(ARG_DEFAULT_TOPIC, defaultTopic)
            args.putLong(ARG_MEETING_ID, meetingId)
            fragment.arguments = args
            return fragment
        }
    }
}