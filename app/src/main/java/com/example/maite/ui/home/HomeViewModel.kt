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
import com.example.maite.model.TimetableEntry
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 홈 화면 ViewModel - 회의 및 제안 관련 데이터 처리
 */
class HomeViewModel(private val proposalRepository: ProposalRepository? = null) : ViewModel() {

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

    init {
        // 시간표 데이터 가져오기
        TimetableDataHolder.timetableEntries
            .onEach { entries ->
                _timetableEntries.postValue(entries)
            }
            .launchIn(viewModelScope)
        
        // 현재 TimetableDataHolder에 한 값이 있으면 즉시 반영
        _timetableEntries.postValue(TimetableDataHolder.timetableEntries.value)
        
        // 제안 데이터를 서버에서 가져오기
        if (proposalRepository != null) {
            loadProposals()
            loadNearestMeeting()
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
                    val result = repository.getUnreadProposals()
                    result.onSuccess { proposals ->
                        Log.d(TAG, "제안 목록 로드 성공: ${proposals.size}개")
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
     * 가장 가까운 회의 가져오기
     */
    fun loadNearestMeeting() {
        proposalRepository?.let { repository ->
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                
                try {
                    android.util.Log.d(TAG, "실제 API로 회의 데이터 가져오기 시도")
                    
                    // 실제 API 호출
                    val result = repository.getMyMeetings()
                    
                    result.onSuccess { meetings ->
                        android.util.Log.d(TAG, "회의 로딩 성공: ${meetings.size}개")
                        
                        if (meetings.isNotEmpty()) {
                            // 가장 가까운 회의 찾기
                            val nearestMeeting = findNearestMeeting(meetings)
                            _nearestMeeting.value = nearestMeeting
                            android.util.Log.d(TAG, "가장 가까운 회의: $nearestMeeting")
                        } else {
                            android.util.Log.d(TAG, "회의가 없습니다.")
                            _nearestMeeting.value = null
                        }
                    }.onFailure { e ->
                        android.util.Log.e(TAG, "회의 로딩 실패", e)
                        _error.value = "회의 정보를 가져오는 중 오류가 발생했습니다: ${e.message}"
                        
                        // API 연동 실패 시 임시 데이터 사용 (테스트용)
                        val todayDate = java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.getDefault()).format(java.util.Date())
                        
                        _nearestMeeting.value = MeetingItem(
                            id = 1,
                            title = "개발 회의 (임시 데이터)",
                            date = todayDate,
                            startTime = "14:00",
                            endTime = "15:00",
                            location = "항공대 과학관 204호"
                        )
                        android.util.Log.d(TAG, "임시 회의 데이터 사용")
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "회의 로딩 중 예외 발생", e)
                    _error.value = "회의 정보를 가져오는 중 오류가 발생했습니다"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
    
    /**
     * 회의 목록에서 가장 가까운 회의를 찾는 함수
     */
    private fun findNearestMeeting(meetings: List<MeetingItem>): MeetingItem? {
        val today = java.util.Calendar.getInstance()
        val formatter = java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.getDefault())
        
        // 시간순 정렬 후 가장 가까운 회의 찾기
        return meetings.sortedBy { meeting -> 
            try {
                val meetingDate = formatter.parse(meeting.date) ?: return@sortedBy Long.MAX_VALUE
                val timeComponents = meeting.startTime.split(":")
                val hour = timeComponents[0].toIntOrNull() ?: 0
                val minute = timeComponents[1].toIntOrNull() ?: 0
                
                val meetingCalendar = java.util.Calendar.getInstance().apply {
                    time = meetingDate
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, minute)
                }
                
                meetingCalendar.timeInMillis - today.timeInMillis
            } catch (e: Exception) {
                android.util.Log.e(TAG, "회의 날짜 파싱 오류", e)
                Long.MAX_VALUE // 오류 발생 시 가장 뒤로 정렬
            }
        }.firstOrNull { meeting ->
            try {
                // 현재 시간 이후의 회의만 고려
                val meetingDate = formatter.parse(meeting.date) ?: return@firstOrNull false
                val timeComponents = meeting.startTime.split(":")
                val hour = timeComponents[0].toIntOrNull() ?: 0
                val minute = timeComponents[1].toIntOrNull() ?: 0
                
                val meetingCalendar = java.util.Calendar.getInstance().apply {
                    time = meetingDate
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, minute)
                }
                
                meetingCalendar.timeInMillis >= today.timeInMillis
            } catch (e: Exception) {
                android.util.Log.e(TAG, "회의 날짜 비교 오류", e)
                false // 오류 발생 시 제외
            }
        }
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
                        } else {
                            // 회의 목록 다시 가져오기
                            loadNearestMeeting()
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
}
