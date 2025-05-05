package com.example.maite.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maite.data.TimetableDataHolder
import com.example.maite.model.TimetableEntry
import com.example.maite.model.UserInfo
import com.example.maite.repository.TimetableRepository
import com.example.maite.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import android.util.Log

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ProfileViewModel"

    private val timetableRepository = TimetableRepository(application)
    private val userRepository = UserRepository(application)

    // 현재 사용자 ID 저장
    private var currentUserId: Long? = null

    // 사용자 정보
    private val _userInfo = MutableLiveData<UserInfo>().apply {
        value = UserInfo(
            name = "김정훈",
            mateCount = 100,
            profileImageUrl = null // TODO: 서버 응답에 따른 프로필 이미지 URL 반영
        )
    }
    val userInfo: LiveData<UserInfo> = _userInfo

    // 시간표 데이터 (초기 상태는 빈 리스트)
    private val _timetable = MutableLiveData<List<TimetableEntry>>(emptyList())
    val timetable: LiveData<List<TimetableEntry>> = _timetable

    // 시간표 수정 관련 이벤트
    private val _timetableEvent = MutableSharedFlow<TimetableEvent>()
    val timetableEvent: SharedFlow<TimetableEvent> = _timetableEvent

    // 시간표 항목 추가
    fun addTimetableEntry(entry: TimetableEntry) {
        val currentList = _timetable.value?.toMutableList() ?: mutableListOf()

        // 수정된 시간 충돌 검사 로직
        val conflictingEntry = currentList.find { existing ->
            existing.dayOfWeek == entry.dayOfWeek && (
                    // 새 일정이 기존 일정과 겹치는지 확인
                    (entry.startHour < existing.endHour && entry.endHour > existing.startHour) ||
                            // 기존 일정이 새 일정을 포함하는지 확인
                            (existing.startHour <= entry.startHour && existing.endHour >= entry.endHour) ||
                            // 새 일정이 기존 일정을 포함하는지 확인
                            (entry.startHour <= existing.startHour && entry.endHour >= existing.endHour)
                    )
        }

        if (conflictingEntry != null) {
            // 충돌 시 이벤트 발행
            viewModelScope.launch {
                _timetableEvent.emit(TimetableEvent.Conflict(conflictingEntry, entry))
            }
            return
        }

        // 충돌 없는 경우 추가
        currentList.add(entry)
        _timetable.value = currentList

        // 공유 데이터 홀더 업데이트
        TimetableDataHolder.updateTimetable(currentList)

        // 성공 이벤트 발행
        viewModelScope.launch {
            _timetableEvent.emit(TimetableEvent.Added(entry))
        }
    }

    // 시간표 항목 삭제
    fun removeTimetableEntry(entry: TimetableEntry) {
        val currentList = _timetable.value?.toMutableList() ?: mutableListOf()
        if (currentList.remove(entry)) {
            _timetable.value = currentList

            // 공유 데이터 홀더 업데이트
            TimetableDataHolder.updateTimetable(currentList)

            viewModelScope.launch {
                _timetableEvent.emit(TimetableEvent.Removed(entry))
            }
        }
    }

    // 충돌 시 덮어쓰기
    fun overwriteConflictingEntry(newEntry: TimetableEntry) {
        val currentList = _timetable.value?.toMutableList() ?: mutableListOf()

        // 충돌하는 항목 찾아서 제거
        val iterator = currentList.iterator()
        while (iterator.hasNext()) {
            val existing = iterator.next()
            if (existing.dayOfWeek == newEntry.dayOfWeek && (
                        (newEntry.startHour < existing.endHour && newEntry.endHour > existing.startHour) ||
                                (existing.startHour <= newEntry.startHour && existing.endHour >= newEntry.endHour) ||
                                (newEntry.startHour <= existing.startHour && newEntry.endHour >= existing.endHour)
                        )) {
                iterator.remove()
            }
        }

        // 새 항목 추가
        currentList.add(newEntry)
        _timetable.value = currentList

        // 공유 데이터 홀더 업데이트
        TimetableDataHolder.updateTimetable(currentList)

        viewModelScope.launch {
            _timetableEvent.emit(TimetableEvent.Added(newEntry))
        }
    }

    // 시간표 초기화
    fun clearTimetable() {
        _timetable.value = emptyList()

        // 공유 데이터 홀더 업데이트
        TimetableDataHolder.updateTimetable(emptyList())
        
        Log.d(TAG, "시간표 초기화 완료")

        viewModelScope.launch {
            _timetableEvent.emit(TimetableEvent.Cleared)
        }
    }

    // 시간표 전체 업데이트 (임시 -> 실제)
    fun updateTimetable(entries: List<TimetableEntry>) {
        _timetable.value = entries.toList()

        // 공유 데이터 홀더 업데이트
        TimetableDataHolder.updateTimetable(entries)

        viewModelScope.launch {
            _timetableEvent.emit(TimetableEvent.SavedToServer)
        }
    }

    // 서버에 시간표 저장 - userId 파라미터 추가
    fun saveTimetableToServer(userId: Long) {
        currentUserId = userId
        viewModelScope.launch {
            try {
                val success = timetableRepository.saveTimetable(userId, _timetable.value ?: emptyList())
                if (success) {
                    _timetableEvent.emit(TimetableEvent.SavedToServer)
                } else {
                    _timetableEvent.emit(TimetableEvent.Error("시간표 저장에 실패했습니다"))
                }
            } catch (e: Exception) {
                _timetableEvent.emit(TimetableEvent.Error("서버에 저장하는 중 오류가 발생했습니다: ${e.message}"))
            }
        }
    }

    // 서버에서 시간표 로드 - userId 파라미터 추가
    fun loadTimetableFromServer(userId: Long) {
        currentUserId = userId
        Log.d(TAG, "loadTimetableFromServer called with userId: $userId")
        
        viewModelScope.launch {
            try {
                // 로딩 이벤트 발행
                _timetableEvent.emit(TimetableEvent.Loading("시간표를 불러오는 중..."))
                
                // 3번 연속 시도 (동기화 신뢰성 향상)
                var serverData = emptyList<TimetableEntry>()
                var retryCount = 0
                var lastError: Exception? = null
                
                while (serverData.isEmpty() && retryCount < 3) {
                    try {
                        // 시간표 로드 시도
                        serverData = timetableRepository.loadTimetable(userId)
                        if (serverData.isNotEmpty()) {
                            Log.d(TAG, "[시도 ${retryCount + 1}] 시간표 로드 성공: ${serverData.size}개 항목")
                            break
                        }
                        Log.w(TAG, "[시도 ${retryCount + 1}] 빈 시간표가 로드됨, 재시도...")
                        retryCount++
                        kotlinx.coroutines.delay(500) // 재시도 전 약간의 지연
                    } catch (e: Exception) {
                        lastError = e
                        Log.e(TAG, "[시도 ${retryCount + 1}] 시간표 로드 오류", e)
                        retryCount++
                        kotlinx.coroutines.delay(500) // 재시도 전 약간의 지연
                    }
                }
                
                // 로드된 데이터가 있으면 처리
                if (serverData.isNotEmpty()) {
                    Log.d(TAG, "Loaded ${serverData.size} timetable entries from repository")
                    
                    // 로드된 데이터 상세 로그
                    serverData.forEach { entry ->
                        Log.d(TAG, "Entry: ${entry.title} on day ${entry.dayOfWeek} from ${entry.startHour}:${entry.startMinute} to ${entry.endHour}:${entry.endMinute}")
                    }
                    
                    // ViewModel 상태 업데이트
                    _timetable.postValue(serverData)  // postValue 사용
                    
                    // 중요: DataHolder 업데이트 전에 로그 추가
                    Log.d(TAG, "Updating DataHolder with ${serverData.size} entries")
                    TimetableDataHolder.updateTimetable(serverData)
                    
                    // 업데이트 후 DataHolder 상태 확인
                    Log.d(TAG, "DataHolder now has ${TimetableDataHolder.timetableEntries.value.size} entries")
                    
                    // 성공 이벤트 발행
                    _timetableEvent.emit(TimetableEvent.Loaded(serverData.size))
                } else {
                    // 로드 실패 - 이벤트 발행
                    if (lastError != null) {
                        Log.e(TAG, "모든 시도 후 시간표 로드 실패", lastError)
                        _timetableEvent.emit(TimetableEvent.Error("시간표를 가져올 수 없습니다: ${lastError.message}"))
                    } else {
                        Log.w(TAG, "모든 시도 후 빈 시간표 반환됨")
                        _timetable.postValue(emptyList())  // 빈 시간표 설정
                        TimetableDataHolder.updateTimetable(emptyList())  // DataHolder도 업데이트
                        _timetableEvent.emit(TimetableEvent.Loaded(0))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "서버에서 로드하는 중 오류 발생", e)
                _timetableEvent.emit(TimetableEvent.Error("서버에서 로드하는 중 오류가 발생했습니다: ${e.message}"))
            }
        }
    }

    // 사용자 정보 로드 메서드 추가
    fun loadUserInfo(userId: Long) {
        currentUserId = userId
        viewModelScope.launch {
            try {
                val userInfo = userRepository.getUserInfo(userId)
                if (userInfo != null) {
                    _userInfo.value = userInfo
                } else {
                    _timetableEvent.emit(TimetableEvent.Error("사용자 정보를 찾을 수 없습니다"))
                }
            } catch (e: Exception) {
                _timetableEvent.emit(TimetableEvent.Error("사용자 정보를 로드하는 중 오류가 발생했습니다: ${e.message}"))
            }
        }
    }

    // 시간표 관련 이벤트 봉인 클래스
    sealed class TimetableEvent {
        data class Added(val entry: TimetableEntry) : TimetableEvent()
        data class Removed(val entry: TimetableEntry) : TimetableEvent()
        data class Conflict(val existing: TimetableEntry, val new: TimetableEntry) : TimetableEvent()
        object Cleared : TimetableEvent()
        object SavedToServer : TimetableEvent()
        data class Error(val message: String) : TimetableEvent()
        // 시간표 로딩 중 상태를 위한 이벤트 추가
        data class Loading(val message: String) : TimetableEvent()
        // 시간표 로딩 완료 이벤트 추가
        data class Loaded(val count: Int) : TimetableEvent()
        // 동기화 관련 이벤트 추가
        data class SyncStarted(val message: String) : TimetableEvent()
        data class SyncCompleted(val success: Boolean, val message: String) : TimetableEvent()
    }
}