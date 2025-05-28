package com.example.maite.ui.profile

import android.app.Application
import android.net.Uri
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
import com.example.maite.repository.MateRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.example.maite.PreferencesUtil
import com.example.maite.util.TimetableColorManager

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ProfileViewModel"

    private val timetableRepository = TimetableRepository(application)
    private val userRepository = UserRepository(application)
    private val mateRepository = MateRepository(application)
    private val preferencesUtil = PreferencesUtil(application)
    private val colorManager = TimetableColorManager(application)

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
        
        // 색상 매핑도 초기화
        colorManager.clearColorMappings()
        
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

    // 서버에 시간표 저장 - userId 파라미터 추가 (성공 여부 반환)
    suspend fun saveTimetableToServer(userId: Long): Boolean {
        currentUserId = userId
        try {
            // 저장 시작 이벤트 발행
            _timetableEvent.emit(TimetableEvent.SyncStarted("서버에 시간표 저장 중..."))
            
            Log.d(TAG, "서버 저장 시도: userId=$userId, 항목 수=${_timetable.value?.size ?: 0}")
            
            val success = timetableRepository.saveTimetable(userId, _timetable.value ?: emptyList())
            
            if (success) {
                _timetableEvent.emit(TimetableEvent.SavedToServer)
                Log.d(TAG, "서버 저장 성공!")
            } else {
                _timetableEvent.emit(TimetableEvent.Error("시간표 저장에 실패했습니다"))
                Log.e(TAG, "서버 저장 실패")
            }
            
            // 저장 완료 이벤트 발행
            _timetableEvent.emit(TimetableEvent.SyncCompleted(success, if (success) "저장 완료" else "저장 실패"))
            
            return success
        } catch (e: Exception) {
            Log.e(TAG, "서버 저장 중 예외 발생", e)
            _timetableEvent.emit(TimetableEvent.Error("서버에 저장하는 중 오류가 발생했습니다: ${e.message}"))
            _timetableEvent.emit(TimetableEvent.SyncCompleted(false, "저장 중 오류 발생"))
            return false
        }
    }

    // 서버에서 시간표 로드 - userId 파라미터 추가
    fun loadTimetableFromServer(userId: Long) {
        currentUserId = userId
        Log.d(TAG, "=== 시간표 로드 시작 === userId: $userId")
        
        viewModelScope.launch {
            try {
                // 로딩 이벤트 발행
                _timetableEvent.emit(TimetableEvent.Loading("시간표를 불러오는 중..."))
                
                Log.d(TAG, "서버에서 시간표 로드 시도")
                val serverData = timetableRepository.loadTimetable(userId)
                
                Log.d(TAG, "서버에서 ${serverData.size}개 시간표 항목 로드")
                
                // 로드된 데이터 상세 로그
                if (serverData.isNotEmpty()) {
                    serverData.forEach { entry ->
                        Log.d(TAG, "항목: ${entry.title}, 요일=${entry.dayOfWeek}, 시간=${entry.startHour}:${entry.startMinute}-${entry.endHour}:${entry.endMinute}")
                    }
                } else {
                    Log.d(TAG, "서버에서 빈 시간표 로드됨 - 새로운 사용자이거나 아직 시간표가 없음")
                }
                
                // ViewModel 상태 업데이트
                _timetable.postValue(serverData)
                
                // TimetableDataHolder 업데이트
                Log.d(TAG, "TimetableDataHolder 업데이트: ${serverData.size}개 항목")
                TimetableDataHolder.updateTimetable(serverData)
                
                // 성공 이벤트 발행
                _timetableEvent.emit(TimetableEvent.Loaded(serverData.size))
                
                Log.d(TAG, "=== 시간표 로드 완료 ===")
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
                // 사용자 정보 조회
                val userInfo = userRepository.getUserInfo(userId)
                
                if (userInfo != null) {
                    // 실제 친구 수 조회
                    val mateCount = mateRepository.getMateCount()
                    Log.d(TAG, "실제 친구 수: $mateCount")
                    
                    // 실제 친구 수를 포함한 새 UserInfo 객체 생성
                    val updatedUserInfo = userInfo.copy(mateCount = mateCount)
                    _userInfo.value = updatedUserInfo
                } else {
                    _timetableEvent.emit(TimetableEvent.Error("사용자 정보를 찾을 수 없습니다"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "사용자 정보 로드 중 오류", e)
                _timetableEvent.emit(TimetableEvent.Error("사용자 정보를 로드하는 중 오류가 발생했습니다: ${e.message}"))
            }
        }
    }
    
    // 프로필 이미지 업로드 기능
    fun uploadProfileImage(imageUri: Uri, fileName: String): LiveData<Boolean> {
        val resultLiveData = MutableLiveData<Boolean>()
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "프로필 이미지 업로드 시작: $fileName")
                
                // 사용자 ID 확인 - 현재 로그인한 사용자의 ID를 사용
                val userId = preferencesUtil.getUserId()
                if (userId == null || userId == 0L) {
                    Log.e(TAG, "프로필 이미지 업로드 실패: 사용자 ID가 없습니다. 테스트 ID 1 사용")
                    // 테스트용 가상 사용자 ID 사용
                    val testUserId = 1L
                    val success = userRepository.uploadProfileImage(testUserId, imageUri, fileName)
                    
                    if (success) {
                        Log.d(TAG, "프로필 이미지 업로드 성공 (테스트 ID 사용)")
                        loadUserInfo(testUserId)
                        resultLiveData.postValue(true)
                    } else {
                        Log.e(TAG, "프로필 이미지 업로드 실패 (테스트 ID 사용)")
                        resultLiveData.postValue(false)
                    }
                    return@launch
                }
                
                // 이미지 업로드 요청
                val success = userRepository.uploadProfileImage(userId, imageUri, fileName)
                
                if (success) {
                    Log.d(TAG, "프로필 이미지 업로드 성공")
                    
                    // 사용자 정보 새로고침 (이미지 URL 업데이트를 위해)
                    loadUserInfo(userId)
                    
                    resultLiveData.postValue(true)
                } else {
                    Log.e(TAG, "프로필 이미지 업로드 실패")
                    resultLiveData.postValue(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "프로필 이미지 업로드 중 오류 발생", e)
                resultLiveData.postValue(false)
            }
        }
        
        return resultLiveData
    }
    
    // 프로필 이미지 업로드 기능 (URL 반환 버전)
    fun uploadProfileImageWithUrl(imageUri: Uri, fileName: String): LiveData<Pair<Boolean, String?>> {
        val resultLiveData = MutableLiveData<Pair<Boolean, String?>>()
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "프로필 이미지 업로드 시작 (URL 반환 버전): $fileName")
                
                // 사용자 ID 확인
                val userId = preferencesUtil.getUserId()
                if (userId == null || userId == 0L) {
                    Log.e(TAG, "프로필 이미지 업로드 실패: 사용자 ID가 없습니다. 테스트 ID 1 사용")
                    val testUserId = 1L
                    val result = userRepository.uploadProfileImageWithUrl(testUserId, imageUri, fileName)
                    resultLiveData.postValue(result)
                    return@launch
                }
                
                // 이미지 업로드 요청
                val result = userRepository.uploadProfileImageWithUrl(userId, imageUri, fileName)
                resultLiveData.postValue(result)
                
            } catch (e: Exception) {
                Log.e(TAG, "프로필 이미지 업로드 예외: ${e.message}")
                resultLiveData.postValue(Pair(false, null))
            }
        }
        
        return resultLiveData
    }

    // 회원가입용 프로필 이미지 업로드 (AuthAPI 사용)
    fun uploadSignupProfileImage(imageUri: Uri, fileName: String): LiveData<Pair<Boolean, String?>> {
        val resultLiveData = MutableLiveData<Pair<Boolean, String?>>()
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "회원가입 프로필 이미지 업로드 시작: $fileName")
                
                // AuthAPI를 통한 이미지 업로드 요청
                val result = userRepository.uploadSignupProfileImage(imageUri, fileName)
                resultLiveData.postValue(result)
                
            } catch (e: Exception) {
                Log.e(TAG, "회원가입 프로필 이미지 업로드 예외: ${e.message}")
                resultLiveData.postValue(Pair(false, null))
            }
        }
        
        return resultLiveData
    }
    
    // 임시 프로필 이미지 URI 저장 기능
    fun setTempProfileImageUri(uri: String) {
        Log.d(TAG, "임시 프로필 이미지 URI 저장: $uri")
        // ViewModel 내부에도 임시 URI 저장 (중요한 상태 공유)
        preferencesUtil.setString("user_profile_image_uri_temp", uri)
        
        // 사용자 정보 업데이트를 통해 이미지 URI를 즉시 적용해볼 수 있음
        val currentUserInfo = _userInfo.value
        if (currentUserInfo != null) {
            // 현재 사용자 정보에 프로필 이미지 URI 임시 적용 (미리보기용)
            val updatedUserInfo = currentUserInfo.copy(profileImageUrl = uri)
            _userInfo.postValue(updatedUserInfo)
            Log.d(TAG, "임시 프로필 이미지 URI가 적용된 사용자 정보 업데이트")
        }
    }
    
    // 프로필 이미지 초기화 기능
    fun resetProfileImage(): LiveData<Boolean> {
        val resultLiveData = MutableLiveData<Boolean>()
        
        viewModelScope.launch {
            try {
                Log.d(TAG, "프로필 이미지 초기화 시작")
                
                // 사용자 ID 확인 - 현재 로그인한 사용자의 ID를 사용
                val userId = preferencesUtil.getUserId()
                if (userId == null || userId == 0L) {
                    Log.e(TAG, "프로필 이미지 초기화 실패: 사용자 ID가 없습니다. 테스트 ID 1 사용")
                    // 테스트용 가상 사용자 ID 사용
                    val testUserId = 1L
                    val success = userRepository.resetProfileImage(testUserId)
                    
                    if (success) {
                        Log.d(TAG, "프로필 이미지 초기화 성공 (테스트 ID 사용)")
                        loadUserInfo(testUserId)
                        resultLiveData.postValue(true)
                    } else {
                        Log.e(TAG, "프로필 이미지 초기화 실패 (테스트 ID 사용)")
                        resultLiveData.postValue(false)
                    }
                    return@launch
                }
                
                // 초기화 요청
                val success = userRepository.resetProfileImage(userId)
                
                if (success) {
                    Log.d(TAG, "프로필 이미지 초기화 성공")
                    
                    // 사용자 정보 새로고침
                    loadUserInfo(userId)
                    
                    resultLiveData.postValue(true)
                } else {
                    Log.e(TAG, "프로필 이미지 초기화 실패")
                    resultLiveData.postValue(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "프로필 이미지 초기화 중 오류 발생", e)
                resultLiveData.postValue(false)
            }
        }
        
        return resultLiveData
    }

    // 실시간 시간표 항목 추가 (즉시 서버 저장)
    fun addTimetableEntryToServer(entry: TimetableEntry) {
        val currentList = _timetable.value?.toMutableList() ?: mutableListOf()

        // 충돌 검사
        val conflictingEntry = currentList.find { existing ->
            existing.dayOfWeek == entry.dayOfWeek && (
                    (entry.startHour < existing.endHour && entry.endHour > existing.startHour) ||
                            (existing.startHour <= entry.startHour && existing.endHour >= entry.endHour) ||
                            (entry.startHour <= existing.startHour && entry.endHour >= existing.endHour)
                    )
        }

        if (conflictingEntry != null) {
            viewModelScope.launch {
                _timetableEvent.emit(TimetableEvent.Conflict(conflictingEntry, entry))
            }
            return
        }

        // 충돌 없는 경우 서버에 추가
        viewModelScope.launch {
            try {
                val userId = currentUserId ?: run {
                    _timetableEvent.emit(TimetableEvent.Error("사용자 ID가 설정되지 않았습니다"))
                    return@launch
                }
                
                _timetableEvent.emit(TimetableEvent.SyncStarted("일정을 추가하는 중..."))
                
                val success = timetableRepository.addEventToServer(userId, entry)
                
                if (success) {
                    // 로컬 상태 업데이트
                    currentList.add(entry)
                    _timetable.value = currentList
                    TimetableDataHolder.updateTimetable(currentList)
                    
                    _timetableEvent.emit(TimetableEvent.Added(entry))
                    _timetableEvent.emit(TimetableEvent.SyncCompleted(true, "일정이 추가되었습니다"))
                    Log.d(TAG, "실시간 일정 추가 성공: ${entry.title}")
                } else {
                    _timetableEvent.emit(TimetableEvent.Error("일정 추가에 실패했습니다"))
                    _timetableEvent.emit(TimetableEvent.SyncCompleted(false, "일정 추가 실패"))
                    Log.e(TAG, "실시간 일정 추가 실패: ${entry.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "실시간 일정 추가 중 예외 발생", e)
                _timetableEvent.emit(TimetableEvent.Error("일정 추가 중 오류가 발생했습니다: ${e.message}"))
                _timetableEvent.emit(TimetableEvent.SyncCompleted(false, "일정 추가 중 오류 발생"))
            }
        }
    }

    // 실시간 시간표 항목 삭제 (즉시 서버 삭제)
    fun removeTimetableEntryFromServer(entry: TimetableEntry) {
        val currentList = _timetable.value?.toMutableList() ?: mutableListOf()
        
        if (!currentList.contains(entry)) {
            Log.w(TAG, "삭제하려는 항목이 로컬에 없습니다: ${entry.title}")
            return
        }

        viewModelScope.launch {
            try {
                val userId = currentUserId ?: run {
                    _timetableEvent.emit(TimetableEvent.Error("사용자 ID가 설정되지 않았습니다"))
                    return@launch
                }
                
                _timetableEvent.emit(TimetableEvent.SyncStarted("일정을 삭제하는 중..."))
                
                val eventId = entry.id ?: run {
                    _timetableEvent.emit(TimetableEvent.Error("일정 ID가 없어 삭제할 수 없습니다"))
                    return@launch
                }
                
                val success = timetableRepository.deleteEventFromServer(userId, eventId)
                
                if (success) {
                    // 로컬 상태 업데이트
                    currentList.remove(entry)
                    _timetable.value = currentList
                    TimetableDataHolder.updateTimetable(currentList)
                    
                    _timetableEvent.emit(TimetableEvent.Removed(entry))
                    _timetableEvent.emit(TimetableEvent.SyncCompleted(true, "일정이 삭제되었습니다"))
                    Log.d(TAG, "실시간 일정 삭제 성공: ${entry.title}")
                } else {
                    _timetableEvent.emit(TimetableEvent.Error("일정 삭제에 실패했습니다"))
                    _timetableEvent.emit(TimetableEvent.SyncCompleted(false, "일정 삭제 실패"))
                    Log.e(TAG, "실시간 일정 삭제 실패: ${entry.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "실시간 일정 삭제 중 예외 발생", e)
                _timetableEvent.emit(TimetableEvent.Error("일정 삭제 중 오류가 발생했습니다: ${e.message}"))
                _timetableEvent.emit(TimetableEvent.SyncCompleted(false, "일정 삭제 중 오류 발생"))
            }
        }
    }

    // 실시간 시간표 초기화 (즉시 서버 삭제)
    fun clearTimetableFromServer() {
        viewModelScope.launch {
            try {
                val userId = currentUserId ?: run {
                    _timetableEvent.emit(TimetableEvent.Error("사용자 ID가 설정되지 않았습니다"))
                    return@launch
                }
                
                _timetableEvent.emit(TimetableEvent.SyncStarted("시간표를 초기화하는 중..."))
                
                val success = timetableRepository.clearAllEventsFromServer(userId)
                
                if (success) {
                    // 로컬 상태 업데이트
                    _timetable.value = emptyList()
                    TimetableDataHolder.updateTimetable(emptyList())
                    colorManager.clearColorMappings()
                    
                    _timetableEvent.emit(TimetableEvent.Cleared)
                    _timetableEvent.emit(TimetableEvent.SyncCompleted(true, "시간표가 초기화되었습니다"))
                    Log.d(TAG, "실시간 시간표 초기화 성공")
                } else {
                    _timetableEvent.emit(TimetableEvent.Error("시간표 초기화에 실패했습니다"))
                    _timetableEvent.emit(TimetableEvent.SyncCompleted(false, "시간표 초기화 실패"))
                    Log.e(TAG, "실시간 시간표 초기화 실패")
                }
            } catch (e: Exception) {
                Log.e(TAG, "실시간 시간표 초기화 중 예외 발생", e)
                _timetableEvent.emit(TimetableEvent.Error("시간표 초기화 중 오류가 발생했습니다: ${e.message}"))
                _timetableEvent.emit(TimetableEvent.SyncCompleted(false, "시간표 초기화 중 오류 발생"))
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