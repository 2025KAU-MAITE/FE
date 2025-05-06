package com.example.maite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.maite.model.MaiteListItem
import com.example.maite.model.MaiteListRepository
import com.example.maite.model.RoomItem
import kotlinx.coroutines.launch

class MaiteListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MaiteListRepository(application)

    private val _maiteList = MutableLiveData<List<MaiteListItem>>()
    val maiteList: LiveData<List<MaiteListItem>> = _maiteList

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // 방 상세 정보를 위한 LiveData 추가
    private val _roomDetail = MutableLiveData<RoomItem>()
    val roomDetail: LiveData<RoomItem> = _roomDetail

    init {
        loadMaiteList()
    }

    fun loadMaiteList() {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                val list = repository.getMaiteList()
                _maiteList.value = list
            } catch (e: Exception) {
                _errorMessage.value = "데이터를 불러오는 중 오류가 발생했습니다: ${e.message}"
                _maiteList.value = emptyList()
            }
        }
    }

    // 방 상세 정보를 가져오는 메서드 추가
    fun getRoomDetail(roomId: Long) {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                val roomDetail = repository.getRoomDetail(roomId)
                _roomDetail.value = roomDetail
            } catch (e: Exception) {
                _errorMessage.value = "방 정보를 불러오는 중 오류가 발생했습니다: ${e.message}"
            }
        }
    }

    fun convertRoomItemToMaiteListItem(roomItem: RoomItem): MaiteListItem {
        return MaiteListItem(
            roomId = roomItem.roomId,
            title = roomItem.name,
            name = roomItem.hostEmail,
            intro = roomItem.description,
            participantEmails = roomItem.participantEmails ?: emptyList()
        )
    }

    fun addNewMaite(maiteListItem: MaiteListItem) {
        val currentList = _maiteList.value?.toMutableList() ?: mutableListOf()
        currentList.add(maiteListItem)
        _maiteList.value = currentList
    }
}