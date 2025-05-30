package com.example.maite.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.maite.model.MeetListItem
import com.example.maite.model.MeetListRepository
import android.util.Log

class MeetListViewModel : ViewModel() {
    // 싱글톤 인스턴스 사용
    private val repository = MeetListRepository.getInstance()

    private val _meetList = MutableLiveData<List<MeetListItem>>()
    val meetList: LiveData<List<MeetListItem>> = _meetList

    init {
        loadMeetList()
    }

    private fun loadMeetList() {
        // Repository에서 데이터 로드
        val data = repository.getMeetList()
        _meetList.value = data
        Log.d("MeetListViewModel", "회의 목록 로드: ${data.size}개 항목")
    }
}