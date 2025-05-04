package com.example.maite.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope // viewModelScope import 추가
import com.example.maite.model.MaiteListItem
import com.example.maite.model.MaiteListRepository
import kotlinx.coroutines.launch // launch import 추가

class MaiteListViewModel : ViewModel() {

    private val repository = MaiteListRepository()

    // LiveData 선언 (기존과 동일)
    private val _maiteList = MutableLiveData<List<MaiteListItem>>()
    val maiteList: LiveData<List<MaiteListItem>> = _maiteList // get() = _maiteList 와 동일

    init {
        // ViewModel 생성 시 데이터 로드 (기존과 동일)
        loadMaiteList()
    }

    // 데이터 로드 함수 수정
    private fun loadMaiteList() {
        // viewModelScope를 사용하여 코루틴 실행
        viewModelScope.launch {
            // Repository의 suspend 함수 호출 (백그라운드 스레드에서 실행)
            val data = repository.getMaiteList()
            // LiveData 값 업데이트 (postValue는 백그라운드 스레드에서도 안전)
            _maiteList.postValue(data)
        }
    }

    // 새 MAITE를 추가하는 기능 (기존 코드 유지)
    fun addNewMaite(maiteListItem: MaiteListItem) {
        val currentList = _maiteList.value?.toMutableList() ?: mutableListOf()
        currentList.add(maiteListItem)
        _maiteList.value = currentList // 메인 스레드에서 LiveData 값 직접 설정
    }
}