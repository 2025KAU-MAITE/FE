package com.example.maite

import android.content.Context // Context import 확인
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
    // lateinit var로 변경하고 초기화 제거
    private lateinit var apiService: MaiteApiService
    private var defaultTopic: String? = null
    private lateinit var loadingDialog: LoadingDialog
    private var uploadJob: Job? = null

    private var originalDimAmount: Float = 0.6f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            defaultTopic = it.getString(ARG_DEFAULT_TOPIC)
        }
        // loadingDialog 초기화는 Context가 필요하므로 onViewCreated나 onAttach 이후로 이동하는 것이 더 안전할 수 있지만,
        // requireContext()가 onCreate에서 일반적으로 안전하게 사용될 수 있으므로 여기 둬도 괜찮습니다.
        // 다만, 만약을 대비해 onViewCreated에서 초기화하는 것을 고려할 수 있습니다.
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

        // 여기서 apiService 초기화 (requireContext() 사용)
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

    // ... (나머지 코드는 동일)

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
        // binding이 null일 수 있는 시점을 고려하여 안전 호출 또는 null 체크 추가
        _binding?.let { b ->
            val isFileSelected = selectedFileUri != null
            val isDoneEnabled = enabled && isFileSelected
            b.doneBtn.isEnabled = isDoneEnabled
            b.doneBtn.isClickable = isDoneEnabled
            b.fileCardView.isEnabled = enabled
            b.fileCardView.isClickable = enabled
            b.titleEditText.isEnabled = enabled
            updateDoneButtonState() // UI 상태 변경 후 버튼 상태 다시 업데이트
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

        setUiEnabled(false)

        dialog?.window?.setDimAmount(0f)

        loadingDialog.show()

        Log.d(TAG, "업로드 시작 (코루틴 실행 전): finalTopic='$finalTopic', uri=$currentSelectedFileUri")

        uploadJob?.cancel()
        uploadJob = viewLifecycleOwner.lifecycleScope.launch {
            var uploadSuccess = false
            var responseMessage: String? = null
            var errorMessage: String? = null

            try {
                // createMultipartBodyPartFromUri 호출 시 safeContext 전달 확인
                val filePart = createMultipartBodyPartFromUri(currentSelectedFileUri, safeContext)
                if (filePart == null) {
                    errorMessage = "파일 처리 중 오류가 발생했습니다."
                    // launch 블록 내에서는 return@launch 사용
                    return@launch
                }

                Log.d(TAG, "API 호출 시작 (코루틴 내부)")
                val response: Response<ResponseBody> = withContext(Dispatchers.IO) {
                    // apiService가 초기화되었으므로 안전하게 사용 가능
                    apiService.uploadAudioSummary(finalTopic, filePart)
                }

                if (!isActive) {
                    Log.w(TAG, "API 호출 후 코루틴 취소됨")
                    errorMessage = "업로드가 취소되었습니다."
                    return@launch
                }

                if (response.isSuccessful) {
                    uploadSuccess = true
                    // response.body()는 null일 수 있으므로 안전 호출 사용
                    responseMessage = response.body()?.string()
                    Log.d(TAG, "업로드 성공: ${response.code()}")
                    // Fragment Result API 사용 시 key, bundle 확인
                    setFragmentResult(REQUEST_KEY_UPLOAD, bundleOf(BUNDLE_KEY_SUCCESS to true, BUNDLE_KEY_RESPONSE to responseMessage))
                } else {
                    val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
                    Log.e(TAG, "업로드 실패: ${response.code()}, 오류: $errorBody")
                    errorMessage = "업로드 실패: ${response.message()}"
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
                // loadingDialog가 초기화되었는지 확인 후 dismiss 호출
                if (::loadingDialog.isInitialized) {
                    loadingDialog.dismiss()
                }

                // 메인 스레드에서 UI 업데이트
                withContext(Dispatchers.Main) {
                    try {
                        dialog?.window?.setDimAmount(originalDimAmount)
                    } catch (e: Exception) {
                        Log.w(TAG, "DimAmount 복원 중 오류 발생", e)
                    }

                    // safeContext가 아직 유효한지 확인 (Fragment가 detach되지 않았는지)
                    if (isAdded && context != null) {
                        if (uploadSuccess) {
                            Toast.makeText(requireContext(), "파일 업로드 성공!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), errorMessage ?: "업로드 처리 중 문제가 발생했습니다.", Toast.LENGTH_LONG).show()
                        }

                        try {
                            setUiEnabled(true) // UI 다시 활성화
                            dismiss()        // BottomSheet 닫기
                        } catch (e: IllegalStateException) {
                            Log.e(TAG, "BottomSheet dismiss 중 오류 발생", e)
                            try {
                                dismissAllowingStateLoss()
                            } catch (ignored: Exception) {}
                        }
                    } else {
                        Log.w(TAG, "Fragment가 detached되어 UI 업데이트 및 dismiss를 건너<0xEB><0x9A><0x9C>니다.")
                    }
                }
            }
        }
    }

    private suspend fun createMultipartBodyPartFromUri(uri: Uri, context: Context): MultipartBody.Part? {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                var fileName: String? = "audio_record.bin" // 기본 파일 이름
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream" // 기본 MIME 타입

                // 파일 이름 가져오기
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val displayName = cursor.getString(nameIndex)
                            // displayName이 null이 아니고 비어있지 않으면 사용
                            if (!displayName.isNullOrBlank()) {
                                fileName = displayName
                            }
                        }
                    }
                }
                Log.d(TAG, "Multipart 생성 - 파일 이름: $fileName, MIME 타입: $mimeType")

                // 파일 내용 읽기 (InputStream 사용 개선)
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    Log.e(TAG, "파일 InputStream을 열 수 없습니다.")
                    return@withContext null
                }

                val fileBytes = inputStream.use { it.readBytes() } // use 블록으로 자동 close 보장

                val requestBody: RequestBody = fileBytes.toRequestBody(
                    mimeType.toMediaTypeOrNull() // MIME 타입 적용
                )

                // MultipartBody.Part 생성
                MultipartBody.Part.createFormData("file", fileName, requestBody)

            } catch (e: Exception) {
                Log.e(TAG, "Multipart 생성 중 오류 발생", e)
                null // 오류 발생 시 null 반환
            }
        }
    }

    private fun updateDoneButtonState() {
        val safeContext = context ?: return
        // _binding이 null이면 아무 작업도 하지 않음 (onDestroyView 이후 호출 방지)
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
        uploadJob?.cancel() // 진행 중인 업로드 작업 취소
        uploadJob = null
        // loadingDialog가 초기화되었고 보여지고 있다면 dismiss
        if (::loadingDialog.isInitialized && loadingDialog.isDialogShowing) {
            loadingDialog.dismiss()
            // dimAmount 복원 시도 (window가 null이 아닐 때만)
            try {
                dialog?.window?.setDimAmount(originalDimAmount)
            } catch (e: Exception) {
                Log.w(TAG, "onDestroyView에서 DimAmount 복원 중 오류 발생", e)
            }
        }
        _binding = null // 메모리 누수 방지를 위해 binding 참조 해제
    }

    companion object {
        const val TAG = "UploadBottomSheet"
        private const val ARG_DEFAULT_TOPIC = "default_topic"
        const val REQUEST_KEY_UPLOAD = "uploadResultRequest"
        const val BUNDLE_KEY_SUCCESS = "uploadSuccess"
        const val BUNDLE_KEY_RESPONSE = "uploadResponse"

        fun newInstance(defaultTopic: String?): UploadBottomSheet {
            val fragment = UploadBottomSheet()
            val args = Bundle()
            // defaultTopic이 null일 수도 있으므로 putString 사용
            args.putString(ARG_DEFAULT_TOPIC, defaultTopic)
            fragment.arguments = args
            return fragment
        }
    }
}