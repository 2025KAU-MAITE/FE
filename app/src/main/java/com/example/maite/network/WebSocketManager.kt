package com.example.maite.network

import android.util.Log
import com.example.maite.PreferencesUtil
import com.example.maite.model.Message
import com.example.maite.model.MessageType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader
import java.text.SimpleDateFormat
import java.util.*

class WebSocketManager private constructor() {
    private val TAG = "WebSocketManager"
    private var stompClient: StompClient? = null
    private val disposables = CompositeDisposable()
    private val subscribers = mutableMapOf<String, MutableList<(Message) -> Unit>>()
    private lateinit var preferencesUtil: PreferencesUtil

    // WebSocket 서버 URL
    private val WS_URL = "ws://3.39.205.32:8080/ws-chat"

    // 서버 연결 상태
    private var isConnecting = false

    companion object {
        @Volatile
        private var instance: WebSocketManager? = null

        fun getInstance(): WebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: WebSocketManager().also { instance = it }
            }
        }
    }

    fun initialize(preferencesUtil: PreferencesUtil) {
        this.preferencesUtil = preferencesUtil

        // 연결이 이미 존재하면 먼저 해제
        disconnect()

        // 새로운 STOMP 클라이언트 생성
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL)

        // 하트비트 설정 (10초마다)
        stompClient?.withClientHeartbeat(10000)?.withServerHeartbeat(10000)

        // 로그 설정
        stompClient?.lifecycle()?.subscribe { lifecycleEvent ->
            when (lifecycleEvent.type) {
                LifecycleEvent.Type.OPENED -> {
                    Log.d(TAG, "===== STOMP 연결 성공 =====")
                    isConnecting = false

                    // 모든 활성 구독 다시 설정
                    resubscribeAll()
                }
                LifecycleEvent.Type.CLOSED -> {
                    Log.d(TAG, "===== STOMP 연결 종료 =====")
                    isConnecting = false
                }
                LifecycleEvent.Type.ERROR -> {
                    Log.e(TAG, "===== STOMP 오류 발생 =====")
                    Log.e(TAG, "오류 내용: ${lifecycleEvent.exception?.message}")
                    lifecycleEvent.exception?.printStackTrace()
                    isConnecting = false

                    // 오류 발생 시 재연결 시도
                    android.os.Handler().postDelayed({
                        if (!isConnected()) {
                            Log.d(TAG, "오류 후 재연결 시도")
                            connect()
                        }
                    }, 5000) // 5초 후 재연결 시도
                }
                else -> {
                    Log.d(TAG, "STOMP 이벤트: ${lifecycleEvent.type}")
                }
            }
        }?.let { disposables.add(it) }

        Log.d(TAG, "WebSocketManager 초기화 완료")
    }

    // 모든 구독 다시 설정
    private fun resubscribeAll() {
        val currentSubscriptions = HashMap(subscribers)
        subscribers.clear() // 기존 구독 정보 초기화

        // 각 채팅방에 대해 구독 다시 설정
        currentSubscriptions.forEach { (roomId, callbacks) ->
            val callbacksCopy = ArrayList(callbacks) // 콜백 복사
            callbacksCopy.forEach { callback ->
                subscribeToChatRoom(roomId, callback)
            }
        }

        Log.d(TAG, "모든 구독 재설정 완료 - 구독 수: ${currentSubscriptions.size}")
    }

    fun connect() {
        if (stompClient == null) {
            Log.e(TAG, "initialize()를 먼저 호출해야 합니다")
            return
        }

        if (stompClient?.isConnected == true) {
            Log.d(TAG, "이미 연결되어 있습니다")
            return
        }

        if (isConnecting) {
            Log.d(TAG, "연결 시도 중입니다")
            return
        }

        isConnecting = true

        // 인증 헤더 설정 - 연결 시에만 필요
        val headers = ArrayList<StompHeader>()
        val accessToken = preferencesUtil.getAccessToken()

        if (!accessToken.isNullOrEmpty()) {
            // 백엔드 요구사항대로 "Bearer " 접두사 추가
            val tokenWithPrefix = "Bearer $accessToken"
            headers.add(StompHeader("Authorization", tokenWithPrefix))

            Log.d(TAG, "===== WebSocket 연결에 인증 토큰 설정 =====")
            Log.d(TAG, "토큰 접두사: Bearer")
            Log.d(TAG, "토큰 길이: ${accessToken.length}")
            Log.d(TAG, "토큰 일부: ${accessToken.take(20)}...")
        } else {
            Log.e(TAG, "인증 토큰이 없습니다. WebSocket 연결이 인증되지 않을 수 있습니다.")
        }

        // 연결 시작
        Log.d(TAG, "===== STOMP 연결 시작: $WS_URL =====")
        stompClient?.connect(headers)
    }

    fun disconnect() {
        try {
            if (stompClient?.isConnected == true) {
                stompClient?.disconnect()
                Log.d(TAG, "STOMP 연결 해제")
            }
            disposables.clear()
            subscribers.clear()
            stompClient = null
            isConnecting = false
        } catch (e: Exception) {
            Log.e(TAG, "연결 해제 중 오류: ${e.message}")
        }
    }

    fun isConnected(): Boolean {
        return stompClient?.isConnected == true
    }

    fun subscribeToChatRoom(roomId: String, callback: (Message) -> Unit) {
        Log.d(TAG, "채팅방 구독 요청: $roomId")

        if (stompClient?.isConnected != true) {
            Log.e(TAG, "STOMP 클라이언트가 연결되지 않았습니다. 연결 시도 후 구독")

            // 먼저 콜백을 등록
            if (!subscribers.containsKey(roomId)) {
                subscribers[roomId] = mutableListOf()
            }
            subscribers[roomId]?.add(callback)

            connect()

            // 연결 후 구독 시도 (1초 대기)
            android.os.Handler().postDelayed({
                if (stompClient?.isConnected == true) {
                    Log.d(TAG, "연결 완료 후 구독 시도: $roomId")
                    subscribeToTopicInternal(roomId)
                } else {
                    Log.e(TAG, "연결 실패로 구독 불가: $roomId")
                }
            }, 1000)

            return
        }

        // 콜백 등록
        if (!subscribers.containsKey(roomId)) {
            subscribers[roomId] = mutableListOf()
        }
        subscribers[roomId]?.add(callback)

        // 구독 실행
        subscribeToTopicInternal(roomId)
    }

    private fun subscribeToTopicInternal(roomId: String) {
        val topic = "/topic/chat/$roomId"
        Log.d(TAG, "===== 채팅방 구독 시작: $topic =====")

        try {
            val disposable = stompClient?.topic(topic)
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe({ topicMessage ->
                    try {
                        val payload = topicMessage.payload
                        Log.d(TAG, "===== 메시지 수신: $topic =====")
                        Log.d(TAG, "페이로드: $payload")

                        // 페이로드가 비어있는지 확인
                        if (payload.isBlank()) {
                            Log.w(TAG, "빈 페이로드 수신됨")
                            return@subscribe
                        }

                        // 페이로드 파싱 시도
                        try {
                            val jsonObject = JSONObject(payload)
                            // JSON 필드 출력
                            val iterator = jsonObject.keys()
                            while (iterator.hasNext()) {
                                val key = iterator.next()
                                val value = jsonObject.opt(key)
                                Log.d(TAG, "JSON 필드: $key = $value")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON 파싱 실패: ${e.message}")
                        }

                        val message = parseMessageFromPayload(payload, roomId)
                        Log.d(TAG, "파싱된 메시지: id=${message.id}, 내용=${message.content}, 발신자=${message.senderId}")

                        // 등록된 모든 콜백에 메시지 전달
                        subscribers[roomId]?.forEach { callback ->
                            try {
                                Log.d(TAG, "콜백 호출 시작: ${callback.hashCode()}")
                                callback(message)
                                Log.d(TAG, "콜백 호출 완료")
                            } catch (e: Exception) {
                                Log.e(TAG, "콜백 실행 중 오류: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "메시지 처리 중 오류: ${e.message}")
                        e.printStackTrace()
                    }
                }, { error ->
                    Log.e(TAG, "구독 오류: ${error.message}")
                    error.printStackTrace()

                    // 오류 발생 시 재구독 시도
                    android.os.Handler().postDelayed({
                        if (stompClient?.isConnected == true) {
                            Log.d(TAG, "구독 재시도: $topic")
                            subscribeToTopicInternal(roomId)
                        }
                    }, 3000) // 3초 후 재시도
                })

            if (disposable != null) {
                disposables.add(disposable)
                Log.d(TAG, "구독 설정 완료: $topic (disposable=${disposable.hashCode()})")
            } else {
                Log.e(TAG, "구독 실패: disposable이 null입니다")
            }
        } catch (e: Exception) {
            Log.e(TAG, "구독 시도 중 예외 발생: ${e.message}")
            e.printStackTrace()
        }
    }

    fun unsubscribeFromChatRoom(roomId: String, callback: (Message) -> Unit) {
        Log.d(TAG, "채팅방 구독 해제: $roomId")
        subscribers[roomId]?.remove(callback)
        Log.d(TAG, "구독 해제 후 남은 콜백 수: ${subscribers[roomId]?.size ?: 0}")
    }

    fun sendMessage(roomId: String, content: String) {
        if (stompClient?.isConnected != true) {
            Log.e(TAG, "STOMP 클라이언트가 연결되지 않았습니다. 연결 시도 후 메시지 전송")
            connect()

            // 연결 후 메시지 전송 시도
            android.os.Handler().postDelayed({
                if (stompClient?.isConnected == true) {
                    Log.d(TAG, "연결 후 메시지 전송: $roomId - $content")
                    sendMessageInternal(roomId, content)
                } else {
                    Log.e(TAG, "연결 실패로 메시지 전송 실패: $roomId - $content")
                }
            }, 2000) // 2초 대기 후 재시도

            return
        }

        sendMessageInternal(roomId, content)
    }

    private fun sendMessageInternal(roomId: String, content: String) {
        try {
            // roomId 타입 변환: String -> Long
            val roomIdLong = try {
                roomId.toLong()
            } catch (e: NumberFormatException) {
                Log.e(TAG, "유효하지 않은 roomId 형식: $roomId. 숫자여야 합니다.", e)
                return
            }

            // userId 가져와서 Long 타입으로 변환
            val userIdInt = preferencesUtil.getUserId() ?: -1
            val userIdLong = userIdInt.toLong()

            // 현재 시간을 ISO 8601 포맷으로 변환 (로컬 시간)
            val currentTime = Calendar.getInstance()
            val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            isoDateFormat.timeZone = TimeZone.getDefault() // 로컬 시간대 사용
            val localIsoTime = isoDateFormat.format(currentTime.time)

            // 디버그 로그
            Log.d(TAG, "메시지 전송 시간(로컬): $localIsoTime")

            // MessageRequestDto 형식에 맞게 메시지 데이터 준비
            val messageJson = JSONObject().apply {
                put("senderId", userIdLong) // DTO에 맞게 senderId를 Long 타입으로 추가
                put("content", content)
                put("sendAt", localIsoTime) // 로컬 시간 추가
                // 토큰 관련 필드 제거 (DTO에 없음)
            }

            val messageStr = messageJson.toString()
            val destination = "/app/chat.sendMessage/$roomIdLong" // Long 타입의 roomId 사용

            // 자세한 디버깅 로그 추가
            Log.d(TAG, "===== 메시지 전송 시작 (타입 변환 후) =====")
            Log.d(TAG, "목적지: $destination")
            Log.d(TAG, "roomId 타입: ${roomIdLong.javaClass.simpleName}")
            Log.d(TAG, "senderId 타입: ${userIdLong.javaClass.simpleName}")
            Log.d(TAG, "메시지 내용: $messageStr")

            // 메시지 전송 (토큰은 WebSocket 연결 시에만 사용됨)
            stompClient?.send(destination, messageStr)
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe({
                    Log.d(TAG, "===== 메시지 전송 성공 =====")
                    Log.d(TAG, "roomId: $roomIdLong, userId: $userIdLong")
                    Log.d(TAG, "내용: $content")
                    Log.d(TAG, "전송 시간: $localIsoTime")

                    // 메시지 저장 확인 로직 (선택적)
                    scheduleMessageStorageCheck(roomIdLong, content)
                }, { error ->
                    Log.e(TAG, "===== 메시지 전송 실패 =====")
                    Log.e(TAG, "오류 유형: ${error.javaClass.simpleName}")
                    Log.e(TAG, "오류 메시지: ${error.message}")
                    error.printStackTrace()
                })?.let { disposable ->
                    disposables.add(disposable)
                }
        } catch (e: Exception) {
            Log.e(TAG, "===== 메시지 전송 중 예외 발생 =====")
            Log.e(TAG, "예외 유형: ${e.javaClass.simpleName}")
            Log.e(TAG, "예외 메시지: ${e.message}")
            e.printStackTrace()
        }
    }

    // 메시지 저장 확인을 위한 메서드
    private fun scheduleMessageStorageCheck(roomId: Long, content: String) {
        // 3초 후 메시지가 실제로 저장되었는지 확인
        android.os.Handler().postDelayed({
            // 현재 구독에 등록된 콜백을 통해 메시지 수신 여부 확인
            Log.d(TAG, "===== 메시지 저장 확인 시작 =====")
            Log.d(TAG, "메시지 내용: $content")

            // 이 시점에서 서버에서 브로드캐스트되어 구독에 의해 수신되었는지 확인할 수 있지만,
            // 추가 검증을 위해 서버에서 최근 메시지를 다시 가져오는 로직을 추가할 수도 있습니다.
            // (채팅방 리포지토리를 통해 최근 메시지를 다시 가져오는 방식)

            // 예: ChatRoomRepository가 접근 가능하다면
            // repository.getLatestMessages(roomId) { messages ->
            //     val found = messages.any { it.content == content }
            //     Log.d(TAG, "메시지 데이터베이스 저장 확인: ${if (found) "성공" else "실패"}")
            // }

            Log.d(TAG, "===== 메시지 저장 확인 완료 =====")
        }, 3000) // 3초 대기
    }

    // 대체 전송 방법 (토큰이 본문에는 있지만 헤더는 없는 방식)
    private fun sendFallbackMessage(destination: String, messageStr: String) {
        try {
            Log.d(TAG, "===== 대체 메시지 전송 시도 (본문에만 토큰 포함) =====")

            // 방법 2: 헤더 없이 메시지만 전송 (토큰은 이미 본문에 포함됨)
            stompClient?.send(destination, messageStr)
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe({
                    Log.d(TAG, "방법 2: 메시지 전송 성공 (본문에만 토큰 포함)")
                }, { error ->
                    Log.e(TAG, "방법 2: 메시지 전송 실패: ${error.message}")
                    error.printStackTrace()
                })?.let { disposable ->
                    disposables.add(disposable)
                }
        } catch (e: Exception) {
            Log.e(TAG, "대체 메시지 전송 중 오류: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun parseMessageFromPayload(payload: String, roomId: String): Message {
        try {
            val jsonObject = JSONObject(payload)

            // 필수 필드 로깅
            val id = jsonObject.optString("id", "")
            val senderId = jsonObject.optString("senderId", "")
            val senderName = jsonObject.optString("senderName", "")
            val content = jsonObject.optString("content", "")
            val sendAt = jsonObject.optString("sendAt", "")

            Log.d(TAG, "파싱: id=$id, senderId=$senderId, name=$senderName, content=$content, sendAt=$sendAt")

            return Message(
                id = id.ifEmpty { UUID.randomUUID().toString() },
                roomId = roomId,
                senderId = senderId,
                senderName = senderName,
                senderProfileImageUrl = jsonObject.optString("senderProfileImageUrl"),
                content = content,
                imageUrl = jsonObject.optString("imageUrl").takeIf { !it.isNullOrBlank() },
                timestamp = parseTimestamp(sendAt),
                readCount = jsonObject.optInt("readCount", 0),
                totalMemberCount = jsonObject.optInt("totalMemberCount", 0),
                isRead = jsonObject.optBoolean("isRead", false),
                type = if (jsonObject.has("imageUrl") && !jsonObject.isNull("imageUrl") &&
                    !jsonObject.optString("imageUrl").isNullOrBlank())
                    MessageType.IMAGE
                else
                    MessageType.TEXT
            )
        } catch (e: Exception) {
            Log.e(TAG, "메시지 파싱 오류: ${e.message}")
            e.printStackTrace()

            // 오류 발생 시 기본 메시지
            return Message(
                id = UUID.randomUUID().toString(),
                roomId = roomId,
                senderId = "-1",
                senderName = "알 수 없음",
                content = "메시지를 표시할 수 없습니다",
                timestamp = System.currentTimeMillis(),
                type = MessageType.TEXT
            )
        }
    }

    private fun parseTimestamp(dateString: String): Long {
        if (dateString.isEmpty()) {
            return System.currentTimeMillis()
        }

        try {
            // 서버 데이터 포맷에 맞춰 파서 설정 (마이크로초 6자리 포함)
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC") // 서버 시간은 UTC로 가정

            val parsedDate = format.parse(dateString)
            return parsedDate?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("TimeDebug", "첫 번째 파싱 실패: ${e.message}")
            try {
                // 밀리초가 없거나 다른 형식으로 시도
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                format.timeZone = TimeZone.getTimeZone("UTC")
                val parsedDate = format.parse(dateString)
                return parsedDate?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                Log.e("TimeDebug", "두 번째 파싱도 실패: ${e2.message}")
                return System.currentTimeMillis()
            }
        }
    }

    fun markMessageAsRead(roomId: String, messageId: String) {
        if (stompClient?.isConnected != true) {
            Log.e(TAG, "STOMP 클라이언트가 연결되지 않았습니다")
            return
        }

        try {
            // 메시지 읽음 처리 데이터 준비
            val readJson = JSONObject().apply {
                put("messageId", messageId)
            }

            // 읽음 처리 메시지 전송
            val destination = "/app/chat.markAsRead/$roomId"
            stompClient?.send(destination, readJson.toString())
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe({
                    Log.d(TAG, "메시지 읽음 처리: $messageId")
                }, { error ->
                    Log.e(TAG, "메시지 읽음 처리 실패: ${error.message}")
                })?.let { disposable ->
                    disposables.add(disposable)
                }
        } catch (e: Exception) {
            Log.e(TAG, "메시지 읽음 처리 오류: ${e.message}")
        }
    }

    // 디버깅을 위한 메서드 추가
    fun debugConnectionStatus() {
        val isConnected = stompClient?.isConnected == true
        Log.d(TAG, "===== WebSocket 연결 상태 =====")
        Log.d(TAG, "연결됨: $isConnected")

        if (isConnected) {
            Log.d(TAG, "활성 구독: ${subscribers.size}개 채팅방")
            subscribers.forEach { (roomId, callbacks) ->
                Log.d(TAG, "채팅방 $roomId: ${callbacks.size}개 콜백")
            }
        }
    }

    // 인증 토큰 확인 메서드
    fun checkAuthentication() {
        val token = preferencesUtil.getAccessToken()
        Log.d(TAG, "===== 인증 토큰 확인 =====")

        if (token.isNullOrEmpty()) {
            Log.e(TAG, "인증 토큰이 없음")
            return
        }

        Log.d(TAG, "토큰 길이: ${token.length}")
        Log.d(TAG, "토큰 일부: ${token.take(20)}...")

        // JWT 토큰 형식 확인
        val parts = token.split(".")
        if (parts.size == 3) {
            Log.d(TAG, "JWT 토큰 형식 확인됨")
        } else {
            Log.e(TAG, "JWT 토큰 형식 아님 (파트 수: ${parts.size})")
        }

        // 연결 상태 확인
        val connected = isConnected()
        Log.d(TAG, "현재 WebSocket 연결 상태: ${if (connected) "연결됨" else "연결 안됨"}")
    }
}