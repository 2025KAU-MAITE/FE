package com.example.maite

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.maite.databinding.AiDialogBinding
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AiDialog(private val context: Context) {

    private lateinit var contentViewBinding: AiDialogBinding
    private val handler = Handler(Looper.getMainLooper())
    private var currentDots = 0
    private val listeningTexts = arrayOf("듣는중.", "듣는중..", "듣는중...")
    private val loadingTexts = arrayOf("분석중.", "분석중..", "분석중...")
    private val respondingTexts = arrayOf("응답중.", "응답중..", "응답중...")

    // 배경 뷰와 콘텐츠 뷰 분리
    private var backgroundView: View? = null
    private var contentView: View? = null
    private var isShowing = false
    private var isProcessing = false // 로딩 상태
    private var isResponding = false // AI 응답 중 상태

    // 펄스 애니메이션 제어 변수
    private var isPulseAnimating = false

    // 오디오 볼륨 감지 관련 변수 (사용자 음성용)
    private var audioRecord: AudioRecord? = null
    private var audioBuffer: ShortArray? = null
    private var isAudioMonitoring = false
    private val audioSampleRate = 44100 // 44.1kHz
    private val audioBufferSize by lazy {
        AudioRecord.getMinBufferSize(
            audioSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
    }

    // AI 응답 음성 모니터링용 MediaPlayer
    private var responseMediaPlayer: MediaPlayer? = null
    private var isResponseAudioMonitoring = false

    // 애니메이션 관련 상수
    private val MIN_SCALE = 1.0f
    private val MAX_SCALE = 1.2f
    private val BASE_ANIMATION_DURATION = 500L // 기본 애니메이션 지속 시간 (ms)

    // 로딩 애니메이션
    private var loadingAnimation: RotateAnimation? = null

    // 원본 이미지 저장
    private var originalImageResource: Int? = null

    // 녹음 상태 콜백
    var onAiCardViewClick: (() -> Unit)? = null

    private val updateListeningText = object : Runnable {
        override fun run() {
            if (isShowing && ::contentViewBinding.isInitialized) {
                when {
                    isResponding -> contentViewBinding.textView15.text = respondingTexts[currentDots]
                    isProcessing -> contentViewBinding.textView15.text = loadingTexts[currentDots]
                    else -> contentViewBinding.textView15.text = listeningTexts[currentDots]
                }
                currentDots = (currentDots + 1) % listeningTexts.size
                handler.postDelayed(this, 500)
            }
        }
    }

    // 사용자 오디오 모니터링을 위한 Runnable
    private val audioLevelMonitor = object : Runnable {
        override fun run() {
            if (isShowing && isAudioMonitoring && audioRecord != null && !isProcessing && !isResponding) {
                val amplitude = getAudioAmplitude()
                updatePulseBasedOnAmplitude(amplitude)
                handler.postDelayed(this, 100) // 100ms마다 업데이트
            }
        }
    }

    // AI 응답 오디오 모니터링을 위한 Runnable
    private val responseAudioLevelMonitor = object : Runnable {
        override fun run() {
            if (isShowing && isResponseAudioMonitoring && responseMediaPlayer != null && isResponding) {
                // MediaPlayer의 볼륨 레벨을 시뮬레이션 (실제 볼륨 감지는 복잡하므로 랜덤 패턴 사용)
                val simulatedAmplitude = generateSimulatedAiAmplitude()
                updatePulseBasedOnAmplitude(simulatedAmplitude)
                handler.postDelayed(this, 150) // 150ms마다 업데이트 (AI 응답은 조금 더 천천히)
            }
        }
    }

    fun show() {
        if (isShowing) return
        val activity = context as? Activity ?: return
        val decorView = activity.window.decorView as ViewGroup
        val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)

        // 뷰들이 null이면 처음 생성
        if (backgroundView == null || contentView == null) {
            // 1. 배경 뷰 생성 및 설정
            backgroundView = FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.parseColor("#99000000"))
                isClickable = true
                isFocusable = true
            }

            // 2. 콘텐츠 뷰 생성
            contentViewBinding = AiDialogBinding.inflate(LayoutInflater.from(context))
            contentView = contentViewBinding.root

            // 원본 이미지 리소스 저장
            originalImageResource = R.drawable.btn_ai

            // aiCardView 클릭 리스너 설정
            contentViewBinding.aiCardView.setOnClickListener {
                if (!isResponding) { // AI 응답 중이 아닐 때만 클릭 가능
                    onAiCardViewClick?.invoke()
                }
            }
        }

        // 이전 뷰 제거
        (backgroundView?.parent as? ViewGroup)?.removeView(backgroundView)
        (contentView?.parent as? ViewGroup)?.removeView(contentView)

        // 루트 뷰에 배경 뷰와 콘텐츠 뷰 순서대로 추가
        rootView.addView(backgroundView)
        rootView.addView(contentView)
        isShowing = true
        isProcessing = false
        isResponding = false

        // 원본 이미지로 복원
        contentViewBinding.btnAi.setImageResource(originalImageResource ?: R.drawable.btn_ai)

        currentDots = 0
        startListeningAnimation()

        // 초기 애니메이션 (확대)
        contentView?.apply {
            scaleX = 0.3f
            scaleY = 0.3f
            alpha = 0f

            animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(400)
                .setInterpolator(OvershootInterpolator(1.2f))
                .withEndAction {
                    // 첫 번째 애니메이션이 끝난 후, 오디오 모니터링 시작
                    startAudioMonitoring()
                }
                .start()
        }

        backgroundView?.apply {
            alpha = 0f
            animate()
                .alpha(1.0f)
                .setDuration(300)
                .start()
        }
    }

    // 로딩 상태 전환 메서드 (부드러운 이미지 전환 추가)
    fun startProcessing() {
        if (!isShowing || !::contentViewBinding.isInitialized) return

        isProcessing = true
        isResponding = false
        stopAudioMonitoring()

        // 모든 애니메이션 정지
        contentViewBinding.aiCardView.clearAnimation()
        contentViewBinding.btnAi.clearAnimation()

        // 원래 크기로 복원
        contentViewBinding.aiCardView.scaleX = 1.0f
        contentViewBinding.aiCardView.scaleY = 1.0f

        // 부드러운 이미지 전환 애니메이션
        smoothImageTransition(R.drawable.loading_spinner) {
            // 이미지 전환 완료 후 로딩 애니메이션 시작
            startLoadingAnimation()
        }
    }

    // AI 응답 상태 전환 메서드 (부드러운 이미지 전환 추가)
    fun startAiResponse(mediaPlayer: MediaPlayer) {
        if (!isShowing || !::contentViewBinding.isInitialized) return

        isProcessing = false
        isResponding = true
        responseMediaPlayer = mediaPlayer

        // 로딩 애니메이션 중지
        stopLoadingAnimation()

        // 부드러운 이미지 전환 애니메이션
        smoothImageTransition(originalImageResource ?: R.drawable.btn_ai) {
            // 이미지 전환 완료 후 AI 응답 오디오 모니터링 시작
            startResponseAudioMonitoring()
        }
    }

    // 부드러운 이미지 전환 메서드
    private fun smoothImageTransition(newImageResource: Int, onComplete: (() -> Unit)? = null) {
        if (!::contentViewBinding.isInitialized) return

        // 페이드 아웃
        contentViewBinding.btnAi.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                // 이미지 변경
                contentViewBinding.btnAi.setImageResource(newImageResource)

                // 페이드 인
                contentViewBinding.btnAi.animate()
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

    // AI 응답 완료
    fun stopAiResponse() {
        isResponding = false
        stopResponseAudioMonitoring()
        responseMediaPlayer = null
    }

    // 로딩 애니메이션 시작
    private fun startLoadingAnimation() {
        if (!::contentViewBinding.isInitialized) return

        // 회전 애니메이션 생성
        loadingAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1200 // 1.2초에 한 바퀴 (조금 천천히)
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator()
        }

        // btnAi에 애니메이션 적용
        contentViewBinding.btnAi.startAnimation(loadingAnimation)
    }

    // 로딩 애니메이션 중지
    private fun stopLoadingAnimation() {
        loadingAnimation?.cancel()
        if (::contentViewBinding.isInitialized) {
            contentViewBinding.btnAi.clearAnimation()
        }
    }

    private fun startListeningAnimation() {
        handler.removeCallbacks(updateListeningText)
        handler.post(updateListeningText)
    }

    // 사용자 오디오 모니터링 시작 메서드
    private fun startAudioMonitoring() {
        if (isShowing && !isAudioMonitoring && !isProcessing && !isResponding) {
            try {
                // 권한 확인
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                    // 권한이 없으면 기본 펄스 애니메이션으로 대체
                    startDefaultPulseAnimation()
                    return
                }

                // 버퍼 크기가 너무 작은지 확인
                val minBufferSize = AudioRecord.getMinBufferSize(
                    audioSampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                if (minBufferSize <= 0) {
                    // 지원되지 않는 오디오 구성
                    startDefaultPulseAnimation()
                    return
                }

                // 버퍼 크기를 최소 버퍼 크기의 2배로 설정
                val bufferSize = minBufferSize * 2
                audioBuffer = ShortArray(bufferSize)

                // 이전 AudioRecord 객체가 있으면 정리
                audioRecord?.release()

                // try-catch로 AudioRecord 생성 감싸기
                try {
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        audioSampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )

                    // 초기화 상태 확인
                    if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                        audioRecord?.startRecording()
                        isAudioMonitoring = true
                        handler.post(audioLevelMonitor)
                    } else {
                        // 초기화 실패
                        audioRecord?.release()
                        audioRecord = null
                        startDefaultPulseAnimation()
                    }
                } catch (e: SecurityException) {
                    // 보안/권한 예외
                    Log.e("AiDialog", "Security exception: ${e.message}")
                    startDefaultPulseAnimation()
                } catch (e: IllegalArgumentException) {
                    // 잘못된 인수 예외
                    Log.e("AiDialog", "Invalid audio configuration: ${e.message}")
                    startDefaultPulseAnimation()
                } catch (e: Exception) {
                    // 기타 예외
                    Log.e("AiDialog", "AudioRecord error: ${e.message}")
                    startDefaultPulseAnimation()
                }
            } catch (e: Exception) {
                Log.e("AiDialog", "Failed to start audio monitoring: ${e.message}")
                startDefaultPulseAnimation()
            }
        }
    }

    // AI 응답 오디오 모니터링 시작
    private fun startResponseAudioMonitoring() {
        if (isShowing && !isResponseAudioMonitoring && isResponding) {
            isResponseAudioMonitoring = true
            handler.post(responseAudioLevelMonitor)
        }
    }

    // AI 응답 오디오 모니터링 중지
    private fun stopResponseAudioMonitoring() {
        isResponseAudioMonitoring = false
        handler.removeCallbacks(responseAudioLevelMonitor)
    }

    // 사용자 오디오 모니터링 중단 메서드
    private fun stopAudioMonitoring() {
        isAudioMonitoring = false
        handler.removeCallbacks(audioLevelMonitor)

        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                try {
                    stop()
                    release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        audioRecord = null
        audioBuffer = null
    }

    // 사용자 오디오 볼륨 레벨(amplitude) 획득 메서드
    private fun getAudioAmplitude(): Float {
        val buffer = audioBuffer ?: return 0f
        val read = audioRecord?.read(buffer, 0, audioBufferSize) ?: 0

        if (read <= 0) return 0f

        // 오디오 샘플에서 최대 진폭 계산
        var sum = 0.0
        for (i in 0 until read) {
            sum += abs(buffer[i].toDouble())
        }
        val average = sum / read

        // 볼륨 레벨을 0~1 범위로 정규화 (적절한 값으로 조정이 필요할 수 있음)
        val normalized = min(1.0, average / 5000.0).toFloat()

        return normalized
    }

    // AI 응답 시뮬레이션 볼륨 생성 (실제 음성 분석 대신 패턴 사용)
    private fun generateSimulatedAiAmplitude(): Float {
        // 음성 패턴을 시뮬레이션하기 위한 랜덤 값 생성
        val baseAmplitude = (0.3f + Math.random().toFloat() * 0.6f) // 0.3 ~ 0.9 범위

        // 주기적인 변화를 위한 사인파 추가
        val time = System.currentTimeMillis() / 100f
        val waveAmplitude = (kotlin.math.sin(time.toDouble()) * 0.2f + 0.8f).toFloat()

        return (baseAmplitude * waveAmplitude).coerceIn(0.1f, 1.0f)
    }

    // 볼륨에 따른 펄스 애니메이션 업데이트
    private fun updatePulseBasedOnAmplitude(amplitude: Float) {
        if (!isShowing || !::contentViewBinding.isInitialized) return

        // 볼륨에 따라 확대 스케일 계산 (MIN_SCALE ~ MAX_SCALE 사이)
        val targetScale = MIN_SCALE + amplitude * (MAX_SCALE - MIN_SCALE)

        // 볼륨에 따라 애니메이션 속도 조절 (큰 소리일수록 빠르게)
        val duration = if (isResponding) {
            // AI 응답 중일 때는 조금 더 부드럽게
            (BASE_ANIMATION_DURATION * (1.2f - amplitude * 0.4f)).toLong()
        } else {
            (BASE_ANIMATION_DURATION * (1.0f - amplitude * 0.5f)).toLong()
        }

        // 확대 애니메이션
        contentViewBinding.aiCardView.animate().cancel() // 이전 애니메이션 취소

        // 볼륨이 일정 임계값 이상일 때만 애니메이션 적용
        val threshold = if (isResponding) 0.1f else 0.05f // AI 응답 중일 때는 더 민감하게
        if (amplitude > threshold) {
            contentViewBinding.aiCardView.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(duration / 2)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    // 확대 후 원래 크기로 복귀
                    if (isShowing) {
                        contentViewBinding.aiCardView.animate()
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

    // 기본 펄스 애니메이션 (오디오 모니터링 실패 시 사용)
    private fun startDefaultPulseAnimation() {
        if (isShowing && ::contentViewBinding.isInitialized && !isPulseAnimating && !isProcessing && !isResponding) {
            isPulseAnimating = true
            runDefaultPulseAnimation()
        }
    }

    private fun runDefaultPulseAnimation() {
        if (!isShowing || !::contentViewBinding.isInitialized || isProcessing || isResponding) {
            isPulseAnimating = false
            return
        }

        // 기본 펄스 애니메이션
        contentViewBinding.aiCardView.animate()
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                contentViewBinding.aiCardView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(500)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        if (isShowing && !isProcessing && !isResponding) {
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
        if (!isShowing) return
        handler.removeCallbacks(updateListeningText)

        // 오디오 모니터링 중지
        stopAudioMonitoring()
        stopResponseAudioMonitoring()
        // 로딩 애니메이션 중지
        stopLoadingAnimation()
        isPulseAnimating = false

        contentView?.animate()
            ?.alpha(0f)
            ?.scaleX(0.3f)
            ?.scaleY(0.3f)
            ?.setDuration(300)
            ?.withEndAction {
                // 애니메이션 완료 후 뷰 제거
                val activity = context as? Activity ?: return@withEndAction
                val decorView = activity.window.decorView as ViewGroup
                val rootView = decorView.findViewById<ViewGroup>(android.R.id.content)

                rootView.removeView(backgroundView)
                rootView.removeView(contentView)

                isShowing = false
                isProcessing = false
                isResponding = false
            }
            ?.start()

        backgroundView?.animate()
            ?.alpha(0f)
            ?.setDuration(300)
            ?.start()
    }

    val isDialogShowing: Boolean
        get() = isShowing
}