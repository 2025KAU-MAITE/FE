package com.example.maite.viewmodel

import android.app.Application // Application import
import androidx.lifecycle.AndroidViewModel // ViewModel 대신 AndroidViewModel 사용
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope // viewModelScope import
import com.example.maite.model.MaiteListItem
import com.example.maite.model.MaiteListRepository
import kotlinx.coroutines.launch // launch import

// Context를 사용하기 위해 AndroidViewModel 상속
class MaiteListViewModel(application: Application) : AndroidViewModel(application) {

    // Repository 생성 시 application context 전달
    private val repository = MaiteListRepository(application)

    private val _maiteList = MutableLiveData<List<MaiteListItem>>()
    val maiteList: LiveData<List<MaiteListItem>> = _maiteList

    // 로딩 상태 LiveData (선택 사항)
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // 오류 메시지 LiveData (선택 사항)
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadMaiteList()
    }

    // API 호출을 위해 viewModelScope 사용
    fun loadMaiteList() {
        viewModelScope.launch {
            _isLoading.value = true // 로딩 시작
            _errorMessage.value = null // 이전 오류 메시지 초기화
            try {
                val list = repository.getMaiteList() // suspend 함수 호출
                _maiteList.value = list
            } catch (e: Exception) {
                // 오류 처리
                _errorMessage.value = "데이터를 불러오는 중 오류가 발생했습니다: ${e.message}"
                _maiteList.value = emptyList() // 오류 시 빈 리스트 설정
            } finally {
                _isLoading.value = false // 로딩 종료
            }
        }
    }

    // 새 MAITE 추가 기능 (이 부분은 API 연동 필요 시 별도 구현)
    fun addNewMaite(maiteListItem: MaiteListItem) {
        // TODO: 이 기능도 API를 통해 서버에 데이터를 추가하고 목록을 새로고침하는 방식으로 변경해야 할 수 있음
        val currentList = _maiteList.value?.toMutableList() ?: mutableListOf()
        currentList.add(maiteListItem)
        _maiteList.value = currentList
    }
}