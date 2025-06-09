package com.example.maite

import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder // MediaRecorder for actual recording is in MeetDetailFragment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.maite.databinding.RecordDialogBinding // XML 파일명에 맞춰 바인딩 클래스명 변경
import kotlin.math.abs
import kotlin.math.min

class RecordDialog(private val context: Context) {

    private var _binding: RecordDialogBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var currentDots = 0
    private val listeningTexts = arrayOf("녹음중.", "녹음중..", "녹음중...")
    private val processingTexts = arrayOf("처리중.", "처리중..", "처리중...") // "분석중" 대신 "처리중"으로 변경

    private var dialog: Dialog? = null
    private var isShowing = false
    private var isProcessing = false // "처리중" 상태 (업로드/요약 단계)

    private var isPulseAnimating = false
    private var audioRecord: AudioRecord? = null // For volume visualization
    private var audioBuffer: ShortArray? = null
    private var isAudioMonitoring = false
    private val audioSampleRate = 44100
    private val audioBufferSize by lazy {
        AudioRecord.getMinBufferSize(
            audioSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
    }

    private val MIN_SCALE = 1.0f
    private val MAX_SCALE = 1.2f
    private val BASE_ANIMATION_DURATION = 500L

    private var processingAnimation: RotateAnimation? = null // Renamed from loadingAnimation
    private var originalImageResource: Int = R.drawable.btn_ai // 기본 녹음 아이콘 (XML의 record ImageView src와 동일하게)

    var onRecordDialogClick: (() -> Unit)? = null // Callback for when the record button in dialog is clicked

    private val updateStatusTextRunnable = object : Runnable {
        override fun run() {
            if (isShowing && _binding != null) {
                when {
                    isProcessing -> binding.textView15.text = processingTexts[currentDots]
                    else -> binding.textView15.text = listeningTexts[currentDots]
                }
                currentDots = (currentDots + 1) % listeningTexts.size
                handler.postDelayed(this, 500)
            }
        }
    }

    private val audioLevelMonitor = object : Runnable {
        override fun run() {
            if (isShowing && isAudioMonitoring && audioRecord != null && !isProcessing) {
                val amplitude = getAudioAmplitude()
                updatePulseBasedOnAmplitude(amplitude)
                handler.postDelayed(this, 100)
            }
        }
    }

    init {
        setupDialog()
    }

    private fun setupDialog() {
        _binding = RecordDialogBinding.inflate(LayoutInflater.from(context))
        dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(binding.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) // 전체 화면으로 설정
            setCancelable(false) // 외부 클릭이나 뒤로가기로 닫히지 않도록 설정 (선택 사항)
            setCanceledOnTouchOutside(false) // 외부 클릭으로 닫히지 않도록 설정 (선택 사항)
        }

        // XML의 ImageView ID가 record 이므로 해당 ID 사용
        binding.record.setImageResource(originalImageResource)
        binding.aiCardView.setOnClickListener { // CardView를 클릭 영역으로 사용
            onRecordDialogClick?.invoke()
        }
    }


    fun show() {
        if (isShowing || dialog == null) return
        isShowing = true
        isProcessing = false

        binding.record.setImageResource(originalImageResource) // 초기 아이콘 설정
        binding.record.clearAnimation() // 이전 애니메이션 제거
        binding.aiCardView.scaleX = 1.0f // 스케일 초기화
        binding.aiCardView.scaleY = 1.0f // 스케일 초기화

        currentDots = 0
        startStatusTextAnimation() // "녹음중..." 애니메이션 시작

        // Dialog 등장 애니메이션 (선택적)
        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(300).withEndAction {
            startAudioMonitoring() // 오디오 시각화 시작
        }.start()

        dialog?.show()
    }

    fun startProcessing() {
        if (!isShowing || _binding == null) return
        isProcessing = true
        stopAudioMonitoring()

        binding.aiCardView.clearAnimation()
        binding.record.clearAnimation()
        binding.aiCardView.scaleX = 1.0f
        binding.aiCardView.scaleY = 1.0f

        smoothImageTransition(R.drawable.loading_spinner) { // 로딩 스피너 아이콘으로 변경
            startProcessingAnimation()
        }
        // 텍스트도 "처리중"으로 업데이트 (updateStatusTextRunnable이 처리)
        currentDots = 0 // 텍스트 애니메이션 초기화
    }


    private fun smoothImageTransition(newImageResource: Int, onComplete: (() -> Unit)? = null) {
        if (_binding == null) return
        binding.record.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                binding.record.setImageResource(newImageResource)
                // tint 설정 (로딩 스피너는 tint가 필요 없을 수 있음, 필요시 조건 추가)
                if (newImageResource == R.drawable.btn_ai) { // 원래 아이콘으로 돌아올 때만 tint
                    binding.record.setColorFilter(ContextCompat.getColor(context, R.color.red_color))
                } else {
                    binding.record.clearColorFilter() // 다른 아이콘은 tint 제거
                }
                binding.record.animate()
                    .alpha(1f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(200)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        onComplete?.invoke()
                    }
                    .start()
            }
            .start()
    }

    private fun startProcessingAnimation() {
        if (_binding == null) return
        processingAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1200
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator()
        }
        binding.record.startAnimation(processingAnimation)
    }

    private fun stopProcessingAnimation() {
        processingAnimation?.cancel()
        _binding?.record?.clearAnimation()
    }

    private fun startStatusTextAnimation() {
        handler.removeCallbacks(updateStatusTextRunnable)
        handler.post(updateStatusTextRunnable)
    }

    private fun startAudioMonitoring() {
        if (isShowing && !isAudioMonitoring && !isProcessing) {
            try {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    startDefaultPulseAnimation()
                    return
                }
                val minBufferSize = AudioRecord.getMinBufferSize(audioSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                if (minBufferSize <= 0) {
                    startDefaultPulseAnimation()
                    return
                }
                val bufferSize = minBufferSize * 2
                audioBuffer = ShortArray(bufferSize)
                audioRecord?.release()

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION, // Or MIC if preferred
                    audioSampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord?.startRecording()
                    isAudioMonitoring = true
                    handler.post(audioLevelMonitor)
                } else {
                    audioRecord?.release()
                    audioRecord = null
                    startDefaultPulseAnimation()
                }
            } catch (e: Exception) {
                Log.e("RecordDialog", "Failed to start audio monitoring: ${e.message}")
                startDefaultPulseAnimation()
            }
        }
    }

    private fun stopAudioMonitoring() {
        isAudioMonitoring = false
        handler.removeCallbacks(audioLevelMonitor)
        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED && recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                try {
                    stop()
                } catch (e: IllegalStateException) { Log.e("RecordDialog", "AudioRecord stop failed", e)}
            }
            release()
        }
        audioRecord = null
        audioBuffer = null
    }

    private fun getAudioAmplitude(): Float {
        val buffer = audioBuffer ?: return 0f
        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0 // Use buffer.size
        if (read <= 0) return 0f
        var sum = 0.0
        for (i in 0 until read) {
            sum += abs(buffer[i].toDouble())
        }
        val average = sum / read
        return min(1.0, average / 2000.0).toFloat() // Normalize
    }

    private fun updatePulseBasedOnAmplitude(amplitude: Float) {
        if (!isShowing || _binding == null || isProcessing) return
        val targetScale = MIN_SCALE + amplitude * (MAX_SCALE - MIN_SCALE)
        val duration = (BASE_ANIMATION_DURATION * (1.0f - amplitude * 0.5f)).toLong()

        binding.aiCardView.animate().cancel()
        val threshold = 0.02f
        if (amplitude > threshold) {
            binding.aiCardView.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(duration / 2)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    if (isShowing && !isProcessing) { // Check isProcessing again
                        binding.aiCardView.animate()
                            .scaleX(MIN_SCALE)
                            .scaleY(MIN_SCALE)
                            .setDuration(duration / 2)
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .start()
                    }
                }
                .start()
        }
    }

    private fun startDefaultPulseAnimation() {
        if (isShowing && _binding != null && !isPulseAnimating && !isProcessing) {
            isPulseAnimating = true
            runDefaultPulseAnimation()
        }
    }

    private fun runDefaultPulseAnimation() {
        if (!isShowing || _binding == null || isProcessing) {
            isPulseAnimating = false
            return
        }
        binding.aiCardView.animate()
            .scaleX(1.08f).scaleY(1.08f).setDuration(600)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                binding.aiCardView.animate()
                    .scaleX(1.0f).scaleY(1.0f).setDuration(600)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        if (isShowing && !isProcessing) {
                            handler.postDelayed({ runDefaultPulseAnimation() }, 300)
                        } else {
                            isPulseAnimating = false
                        }
                    }
                    .start()
            }
            .start()
    }

    fun dismiss() {
        if (!isShowing || dialog == null) return
        isShowing = false
        isProcessing = false // Reset processing state
        handler.removeCallbacks(updateStatusTextRunnable)
        stopAudioMonitoring()
        stopProcessingAnimation()
        isPulseAnimating = false

        // Dialog 소멸 애니메이션 (선택적)
        _binding?.root?.animate()?.alpha(0f)?.setDuration(300)?.withEndAction {
            dialog?.dismiss()
        }?.start() ?: dialog?.dismiss()
    }

    val isDialogShowing: Boolean get() = isShowing
}