package com.example.maite.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.maite.model.MaiteListItem
import com.example.maite.model.MaiteRepository

class MaiteListViewModel : ViewModel() {

    private val repository = MaiteRepository()

    private val _maiteList = MutableLiveData<List<MaiteListItem>>()
    val maiteList: LiveData<List<MaiteListItem>> = _maiteList

    init {
        loadMaiteList()
    }

    private fun loadMaiteList() {
        // 실제 앱에서는 백그라운드 스레드에서 수행될 수 있습니다
        _maiteList.value = repository.getMaiteList()
    }

    // 새 MAITE를 추가하는 기능 (구현 예정)
    fun addNewMaite(maiteListItem: MaiteListItem) {
        val currentList = _maiteList.value?.toMutableList() ?: mutableListOf()
        currentList.add(maiteListItem)
        _maiteList.value = currentList
    }
}