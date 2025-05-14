package com.example.maite

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
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

    // 배경 뷰와 콘텐츠 뷰 분리
    private var backgroundView: View? = null
    private var contentView: View? = null
    private var isShowing = false

    // 펄스 애니메이션 제어 변수
    private var isPulseAnimating = false

    // 오디오 볼륨 감지 관련 변수
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

    // 애니메이션 관련 상수
    private val MIN_SCALE = 1.0f
    private val MAX_SCALE = 1.2f
    private val BASE_ANIMATION_DURATION = 500L // 기본 애니메이션 지속 시간 (ms)

    // 녹음 상태 콜백
    var onAiCardViewClick: (() -> Unit)? = null

    private val updateListeningText = object : Runnable {
        override fun run() {
            if (isShowing && ::contentViewBinding.isInitialized) {
                contentViewBinding.textView15.text = listeningTexts[currentDots]
                currentDots = (currentDots + 1) % listeningTexts.size
                handler.postDelayed(this, 500)
            }
        }
    }

    // 오디오 모니터링을 위한 Runnable
    private val audioLevelMonitor = object : Runnable {
        override fun run() {
            if (isShowing && isAudioMonitoring && audioRecord != null) {
                val amplitude = getAudioAmplitude()
                updatePulseBasedOnAmplitude(amplitude)
                handler.postDelayed(this, 100) // 100ms마다 업데이트
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

            // aiCardView 클릭 리스너 설정
            contentViewBinding.aiCardView.setOnClickListener {
                onAiCardViewClick?.invoke()
            }
        }

        // 이전 뷰 제거
        (backgroundView?.parent as? ViewGroup)?.removeView(backgroundView)
        (contentView?.parent as? ViewGroup)?.removeView(contentView)

        // 루트 뷰에 배경 뷰와 콘텐츠 뷰 순서대로 추가
        rootView.addView(backgroundView)
        rootView.addView(contentView)
        isShowing = true

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

    private fun startListeningAnimation() {
        handler.removeCallbacks(updateListeningText)
        handler.post(updateListeningText)
    }

    // 오디오 모니터링 시작 메서드
    // 오디오 모니터링 시작 메서드
    private fun startAudioMonitoring() {
        if (isShowing && !isAudioMonitoring) {
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

    // 오디오 모니터링 중단 메서드
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

    // 오디오 볼륨 레벨(amplitude) 획득 메서드
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

    // 볼륨에 따른 펄스 애니메이션 업데이트
    private fun updatePulseBasedOnAmplitude(amplitude: Float) {
        if (!isShowing || !::contentViewBinding.isInitialized) return

        // 볼륨에 따라 확대 스케일 계산 (MIN_SCALE ~ MAX_SCALE 사이)
        val targetScale = MIN_SCALE + amplitude * (MAX_SCALE - MIN_SCALE)

        // 볼륨에 따라 애니메이션 속도 조절 (큰 소리일수록 빠르게)
        val duration = (BASE_ANIMATION_DURATION * (1.0f - amplitude * 0.5f)).toLong()

        // 확대 애니메이션
        contentViewBinding.aiCardView.animate().cancel() // 이전 애니메이션 취소

        // 볼륨이 일정 임계값 이상일 때만 애니메이션 적용
        if (amplitude > 0.05f) {
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
        if (isShowing && ::contentViewBinding.isInitialized && !isPulseAnimating) {
            isPulseAnimating = true
            runDefaultPulseAnimation()
        }
    }

    private fun runDefaultPulseAnimation() {
        if (!isShowing || !::contentViewBinding.isInitialized) {
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
                        if (isShowing) {
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