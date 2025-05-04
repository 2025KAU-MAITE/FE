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

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

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
        viewModelScope.launch {
            try {
                val serverData = timetableRepository.loadTimetable(userId)
                _timetable.value = serverData
                TimetableDataHolder.updateTimetable(serverData)
            } catch (e: Exception) {
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
    }
}