package com.example.maite.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maite.data.TimetableDataHolder
import com.example.maite.data.model.MeetingItem
import com.example.maite.data.model.MeetingProposal
import com.example.maite.data.model.ProposalType
import com.example.maite.data.repository.ProposalRepository
import com.example.maite.repository.MeetingRepository
import com.example.maite.model.TimetableEntry
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.os.Handler
import android.os.Looper

/**
 * 홈 화면 ViewModel - 회의 및 제안 관련 데이터 처리
 */
class HomeViewModel(
    private val proposalRepository: ProposalRepository? = null,
    private val meetingRepository: MeetingRepository? = null
) : ViewModel() {

    private val TAG = "HomeViewModel"

    // 내 시간표
    private val _timetableEntries = MutableLiveData<List<TimetableEntry>>(emptyList())
    val timetableEntries: LiveData<List<TimetableEntry>> = _timetableEntries

    // 가장 가까운 회의
    private val _nearestMeeting = MutableLiveData<MeetingItem?>()
    val nearestMeeting: LiveData<MeetingItem?> = _nearestMeeting

    // 제안 목록
    private val _proposals = MutableLiveData<List<MeetingProposal>>(emptyList())
    val proposals: LiveData<List<MeetingProposal>> = _proposals

    // 로딩 상태
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // 오류 메시지
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // 회의방 참가 이벤트 (토스트 메시지용)
    private val _roomJoinEvent = MutableLiveData<String?>()
    val roomJoinEvent: LiveData<String?> = _roomJoinEvent

    // 회의방 이동 이벤트
    private val _navigateToRoomId = MutableLiveData<Int?>()
    val navigateToRoomId: LiveData<Int?> = _navigateToRoomId

    init {
        // 시간표 데이터 가져오기
        TimetableDataHolder.timetableEntries
            .onEach { entries ->
                Log.d(TAG, "TimetableDataHolder Flow에서 시간표 갱신: ${entries.size}개 항목")
                _timetableEntries.postValue(entries)
            }
            .launchIn(viewModelScope)
        
        // 현재 TimetableDataHolder에 값이 있으면 즉시 반영
        val currentEntries = TimetableDataHolder.timetableEntries.value
        Log.d(TAG, "init: TimetableDataHolder에서 초기 시간표 데이터 가져옴 (${currentEntries.size}개 항목)")
        _timetableEntries.postValue(currentEntries)
        
        // 시간표가 비어 있을 경우 새로 불러오는 시도
        viewModelScope.launch {
            // DataHolder에 시간표 데이터가 없는 경우 - 로그 추가
            if (currentEntries.isEmpty()) {
                Log.d(TAG, "TimetableDataHolder가 비어있음. ProfileViewModel에서 시간표 갱신 기다림")
            }
            
            // 기타 데이터 초기화
            if (proposalRepository != null) {
                loadProposals()
                loadNearestMeeting()
            }
        }
    }

    /**
     * 제안 목록 가져오기
     */
    fun loadProposals() {
        proposalRepository?.let { repository ->
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                
                try {
                    Log.d(TAG, "제안 목록 가져오기 (회의 제안 + 회의방 초대) 시작")
                    
                    // 모든 제안(회의 제안 + 회의방 초대)을 함께 가져오기
                    val result = repository.getAllProposals()
                    result.onSuccess { proposals ->
                        Log.d(TAG, "제안 목록 로드 성공: 총 ${proposals.size}개")
                        
                        // 타입별 구분하여 로깅
                        val meetingProposals = proposals.filter { it.type == ProposalType.MEETING }
                        val roomInvites = proposals.filter { it.type == ProposalType.ROOM_INVITE }
                        
                        Log.d(TAG, "회의 제안: ${meetingProposals.size}개, 회의방 초대: ${roomInvites.size}개")
                        
                        // 회의방 초대가 있는 경우 상세 정보 출력
                        if (roomInvites.isNotEmpty()) {
                            roomInvites.forEachIndexed { index, invite ->
                                Log.d(TAG, "회의방 초대[$index] - ID: ${invite.id}, 방ID: ${invite.roomId}, " +
                                        "방이름: ${invite.roomName}, 보낸사람: ${invite.fromUser}")
                            }
                        } else {
                            Log.d(TAG, "읽지 않은 회의방 초대가 없습니다.")
                        }
                        
                        _proposals.value = proposals
                    }.onFailure { e ->
                        Log.e(TAG, "제안 목록 로드 실패", e)
                        _error.value = "제안 목록을 가져오는데 실패했습니다: ${e.message}"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "제안 목록 로드 중 예외 발생", e)
                    _error.value = "제안 목록을 가져오는 중 오류가 발생했습니다"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * 가장 가까운 회의 가져오기 - MeetingRepository 사용으로 개선
     */
    fun loadNearestMeeting() {
        Log.d(TAG, "loadNearestMeeting() 호출")
        
        meetingRepository?.let { repository ->
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                
                try {
                    Log.d(TAG, "MeetingRepository로 회의 데이터 가져오기 시도")
                    
                    // MeetingRepository를 통한 실제 API 호출 (GET /meetings)
                    val result = repository.getMyMeetings()
                    
                    result.onSuccess { meetings ->
                        Log.d(TAG, "회의 로딩 성공: ${meetings.size}개")
                        
                        // 각 회의 상세 정보 로깅
                        meetings.forEachIndexed { index, meeting ->
                            Log.d(TAG, "회의[$index]: ID=${meeting.id}, 제목=${meeting.title}, 날짜=${meeting.date}, 시간=${meeting.startTime}, 장소=${meeting.location}")
                        }
                        
                        if (meetings.isNotEmpty()) {
                            // 가장 가까운 회의 찾기 - 개선된 로직 사용
                            val nearestMeeting = findNearestMeeting(meetings)
                            _nearestMeeting.value = nearestMeeting
                            Log.d(TAG, "가장 가까운 회의: $nearestMeeting")
                        } else {
                            Log.d(TAG, "회의가 없습니다.")
                            _nearestMeeting.value = null
                        }
                    }.onFailure { e ->
                        Log.e(TAG, "회의 로딩 실패", e)
                        _error.value = "회의 정보를 가져오는 중 오류가 발생했습니다: ${e.message}"
                        _nearestMeeting.value = null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "회의 로딩 중 예외 발생", e)
                    _error.value = "회의 정보를 가져오는 중 오류가 발생했습니다"
                    _nearestMeeting.value = null
                } finally {
                    _isLoading.value = false
                }
            }
        } ?: run {
            Log.w(TAG, "MeetingRepository가 null입니다.")
            // MeetingRepository가 없는 경우 fallback으로 ProposalRepository 사용
            proposalRepository?.let { repository ->
                viewModelScope.launch {
                    _isLoading.value = true
                    _error.value = null
                    
                    try {
                        Log.d(TAG, "ProposalRepository로 회의 데이터 가져오기 시도 (fallback)")
                        
                        val result = repository.getMyMeetings()
                        
                        result.onSuccess { meetings ->
                            Log.d(TAG, "회의 로딩 성공: ${meetings.size}개")
                            
                            if (meetings.isNotEmpty()) {
                                val nearestMeeting = findNearestMeeting(meetings)
                                _nearestMeeting.value = nearestMeeting
                                Log.d(TAG, "가장 가까운 회의: $nearestMeeting")
                            } else {
                                Log.d(TAG, "회의가 없습니다.")
                                _nearestMeeting.value = null
                            }
                        }.onFailure { e ->
                            Log.e(TAG, "회의 로딩 실패", e)
                            _error.value = "회의 정보를 가져오는 중 오류가 발생했습니다: ${e.message}"
                            _nearestMeeting.value = null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "회의 로딩 중 예외 발생", e)
                        _error.value = "회의 정보를 가져오는 중 오류가 발생했습니다"
                        _nearestMeeting.value = null
                    } finally {
                        _isLoading.value = false
                    }
                }
            } ?: run {
                Log.e(TAG, "ProposalRepository도 null입니다. 두 레포지토리 모두 사용할 수 없습니다.")
                _error.value = "회의 정보를 가져오는 중 오류가 발생했습니다"
                _nearestMeeting.value = null
            }
        }
    }
    
    /**
     * 회의 목록에서 가장 가까운 회의를 찾는 함수 (개선된 버전)
     * 요구사항: 현재 날짜와 가장 가까운 미래 회의 1개 반환
     * - 과거 회의는 제외
     * - 같은 날짜에 여러 회의가 있으면 시간 기준 가장 가까운 것
     */
    private fun findNearestMeeting(meetings: List<MeetingItem>): MeetingItem? {
        val now = Calendar.getInstance()
        
        // 스웨거에서 받아오는 날짜 형식에 맞춰서 수정! (yyyy-MM-dd)
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        Log.d(TAG, "가장 가까운 회의 찾기 시작. 전체 회의 수: ${meetings.size}")
        
        // 1. 모든 회의를 날짜/시간 순으로 정렬하고, 미래 회의만 필터링
        val futureMeetings = meetings.mapNotNull { meeting ->
            try {
                Log.d(TAG, "회의 처리 중: ${meeting.title}, 날짜: ${meeting.date}, 시간: ${meeting.startTime}")
                
                // 날짜 파싱 (yyyy-MM-dd 형식)
                val meetingDate = dateFormatter.parse(meeting.date)
                if (meetingDate == null) {
                    Log.w(TAG, "날짜 파싱 실패: ${meeting.date}")
                    return@mapNotNull null
                }
                
                Log.d(TAG, "날짜 파싱 성공: ${meeting.date}")
                
                // 시간 파싱
                val timeComponents = meeting.startTime.split(":")
                if (timeComponents.size < 2) {
                    Log.w(TAG, "시간 형식 오류: ${meeting.startTime}")
                    return@mapNotNull null
                }
                
                val hour = timeComponents[0].toIntOrNull() ?: 0
                val minute = timeComponents[1].toIntOrNull() ?: 0
                
                Log.d(TAG, "시간 파싱 성공: ${hour}:${minute}")
                
                // 회의 시간 생성
                val meetingCalendar = Calendar.getInstance().apply {
                    time = meetingDate
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                
                // 현재 시간보다 미래인지 확인
                if (meetingCalendar.timeInMillis <= now.timeInMillis) {
                    Log.d(TAG, "과거 회의 제외: ${meeting.title} (${meeting.date} ${meeting.startTime})")
                    return@mapNotNull null
                }
                
                // 회의와 현재 시간의 차이 계산 (밀리초 단위)
                val timeDifference = meetingCalendar.timeInMillis - now.timeInMillis
                
                Log.d(TAG, "미래 회의 발견: ${meeting.title} (${meeting.date} ${meeting.startTime}), 차이: ${timeDifference}ms")
                
                Pair(meeting, timeDifference)
                
            } catch (e: Exception) {
                Log.e(TAG, "회의 시간 처리 오류: ${meeting.title}", e)
                null
            }
        }.sortedBy { it.second } // 시간 차이로 정렬 (가장 가까운 것부터)
        
        Log.d(TAG, "미래 회의 ${futureMeetings.size}개 발견")
        
        // 2. 가장 가까운 회의 반환
        val nearestMeeting = futureMeetings.firstOrNull()?.first
        
        if (nearestMeeting != null) {
            Log.d(TAG, "가장 가까운 회의 결정: ${nearestMeeting.title} (${nearestMeeting.date} ${nearestMeeting.startTime})")
        } else {
            Log.d(TAG, "미래 회의가 없습니다.")
        }
        
        return nearestMeeting
    }

    /**
     * 제안 수락하기
     */
    fun acceptProposal(proposal: MeetingProposal) {
        proposalRepository?.let { repository ->
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                
                // 신뢰성 있는 UI 경험을 위해 즉시 제안 리스트에서 제거
                val currentProposals = _proposals.value ?: emptyList()
                _proposals.value = currentProposals.filterNot { it.id == proposal.id }
                
                try {
                    val result = repository.acceptProposal(proposal)
                    result.onSuccess {
                        // 제안 타입에 따른 처리
                        if (proposal.type == ProposalType.ROOM_INVITE) {
                            // 회의방 참가 이벤트 발생
                            _roomJoinEvent.value = proposal.roomName
                            
                            // 회의방 ID를 저장하여 ListFragment에서 바로 해당 방으로 이동하기 위한 데이터
                            _navigateToRoomId.value = proposal.roomId
                        } else {
                            // 회의 목록 다시 가져오기 (지연 시간 증가)
                            Handler(Looper.getMainLooper()).postDelayed({
                                loadNearestMeeting()
                            }, 1000) // 1초 대기 후 새로고침
                        }
                    }.onFailure { e ->
                        Log.e(TAG, "제안 수락 실패", e)
                        _error.value = "제안 수락에 실패했습니다: ${e.message}"
                        // 오류 발생 시 제안 목록 다시 가져오기
                        loadProposals()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "제안 수락 중 예외 발생", e)
                    _error.value = "제안을 수락하는 중 오류가 발생했습니다"
                    // 오류 발생 시 제안 목록 다시 가져오기
                    loadProposals()
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * 제안 거절하기
     */
    fun declineProposal(proposal: MeetingProposal) {
        proposalRepository?.let { repository ->
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                
                // 신뢰성 있는 UI 경험을 위해 즉시 제안 리스트에서 제거
                val currentProposals = _proposals.value ?: emptyList()
                _proposals.value = currentProposals.filterNot { it.id == proposal.id }
                
                try {
                    val result = repository.rejectProposal(proposal)
                    result.onFailure { e ->
                        Log.e(TAG, "제안 거절 실패", e)
                        _error.value = "제안 거절에 실패했습니다: ${e.message}"
                        // 오류 발생 시 제안 목록 다시 가져오기
                        loadProposals()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "제안 거절 중 예외 발생", e)
                    _error.value = "제안을 거절하는 중 오류가 발생했습니다"
                    // 오류 발생 시 제안 목록 다시 가져오기
                    loadProposals()
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * 오류 메시지 초기화
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * 회의방 참가 이벤트 초기화
     */
    fun clearRoomJoinEvent() {
        _roomJoinEvent.value = null
    }

    /**
     * 회의방 이동 이벤트 초기화
     */
    fun clearNavigateToRoomId() {
        _navigateToRoomId.value = null
    }
}
