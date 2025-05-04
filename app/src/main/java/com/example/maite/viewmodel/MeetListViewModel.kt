package com.example.maite.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.maite.model.MeetListItem
import com.example.maite.model.MeetListRepository
class MeetListViewModel : ViewModel() {
    private val repository = MeetListRepository()

    private val _meetList = MutableLiveData<List<MeetListItem>>()
    val meetList: LiveData<List<MeetListItem>> = _meetList

    init {
        loadMeetList()
    }

    private fun loadMeetList() {
        // Repository를 통해 데이터 로드
        _meetList.value = repository.getMeetList()
    }
}