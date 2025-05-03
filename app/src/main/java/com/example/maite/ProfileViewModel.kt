package com.example.maite.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maite.TimetableRepository
import com.example.maite.data.TimetableStore
import com.example.maite.model.TimetableEntry
import com.example.maite.model.UserInfo
import com.example.maite.MyRetrofit
import com.example.maite.util.mapper.toEventRequest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import android.util.Log



class ProfileViewModel : ViewModel() {

    private val repository = TimetableRepository(MyRetrofit.timetableApi)

    val userInfo = androidx.lifecycle.MutableLiveData(
        UserInfo(
            name = "김정훈",
            mateCount = 100,
            profileImageUrl = null
        )
    )

    private val _timetableEvent = MutableSharedFlow<TimetableEvent>()
    val timetableEvent: SharedFlow<TimetableEvent> = _timetableEvent

    fun addTimetableEntry(entry: TimetableEntry) {
        val current = TimetableStore.entries.value.orEmpty()
        val conflict = current.find {
            it.dayOfWeek == entry.dayOfWeek && (
                    entry.startHour < it.endHour && entry.endHour > it.startHour ||
                            it.startHour <= entry.startHour && it.endHour >= entry.endHour ||
                            entry.startHour <= it.startHour && entry.endHour >= it.endHour
                    )
        }

        if (conflict != null) {
            viewModelScope.launch { _timetableEvent.emit(TimetableEvent.Conflict(conflict, entry)) }
        } else {
            TimetableStore.add(entry)
            viewModelScope.launch { _timetableEvent.emit(TimetableEvent.Added(entry)) }
        }
    }

    fun overwriteConflictingEntry(newEntry: TimetableEntry) {
        val current = TimetableStore.entries.value.orEmpty().toMutableList()

        val cleared = current.filterNot {
            it.dayOfWeek == newEntry.dayOfWeek && (
                    newEntry.startHour < it.endHour && newEntry.endHour > it.startHour ||
                            it.startHour <= newEntry.startHour && it.endHour >= newEntry.endHour ||
                            newEntry.startHour <= it.startHour && newEntry.endHour >= it.endHour
                    )
        }.toMutableList()

        cleared += newEntry
        TimetableStore.set(cleared)

        viewModelScope.launch { _timetableEvent.emit(TimetableEvent.Added(newEntry)) }
    }

    fun removeTimetableEntry(entry: TimetableEntry) {
        TimetableStore.remove(entry)
        viewModelScope.launch { _timetableEvent.emit(TimetableEvent.Removed(entry)) }
    }

    fun clearTimetable() {
        TimetableStore.clear()
        viewModelScope.launch { _timetableEvent.emit(TimetableEvent.Cleared) }
    }

    fun updateTimetable(entries: List<TimetableEntry>) {
        TimetableStore.set(entries)
        viewModelScope.launch { _timetableEvent.emit(TimetableEvent.SavedToServer) }
    }

    // 🆕 서버에 시간표 저장
    fun saveTimetableToServer(userId: Long) {
        val entries = TimetableStore.entries.value.orEmpty()

        viewModelScope.launch {
            try {
                Log.d("ProfileViewModel", "🛰 요청 시작: 시간표 생성 요청 for userId=$userId")
                val response = repository.createTimetable(userId)

                if (response.isSuccessful) {
                    val timetableId = response.body()?.result?.id
                    Log.d("ProfileViewModel", "✅ 성공: timetableId=$timetableId")

                    if (timetableId != null) {
                        repository.saveTimetable(timetableId, entries)
                        Log.d("ProfileViewModel", "✅ 이벤트 저장 완료")
                        _timetableEvent.emit(TimetableEvent.SavedToServer)
                    } else {
                        Log.e("ProfileViewModel", "❌ timetableId가 null임")
                        _timetableEvent.emit(TimetableEvent.Error("timetableId가 null임"))
                    }
                } else {
                    Log.e("ProfileViewModel", "❌ 시간표 생성 실패: ${response.code()}, ${response.message()}")
                    _timetableEvent.emit(TimetableEvent.Error("시간표 생성 실패: ${response.message()}"))
                }

            } catch (e: Exception) {
                Log.e("ProfileViewModel", "❌ 요청 예외 발생: ${e.localizedMessage}")
                _timetableEvent.emit(TimetableEvent.Error("요청 실패: ${e.localizedMessage}"))
            }
        }
    }



    fun loadTimetableFromServer(userId: Long) {
        viewModelScope.launch {
            try {
                val entries = repository.loadTimetable(userId)
                TimetableStore.set(entries)
                _timetableEvent.emit(TimetableEvent.SavedToServer)
            } catch (e: Exception) {
                _timetableEvent.emit(
                    TimetableEvent.Error("시간표 불러오기 실패: ${e.localizedMessage}")
                )
            }
        }
    }

    sealed class TimetableEvent {
        data class Added(val entry: TimetableEntry) : TimetableEvent()
        data class Removed(val entry: TimetableEntry) : TimetableEvent()
        data class Conflict(val existing: TimetableEntry, val new: TimetableEntry) : TimetableEvent()
        object Cleared : TimetableEvent()
        object SavedToServer : TimetableEvent()
        data class Error(val message: String) : TimetableEvent()
    }
}
