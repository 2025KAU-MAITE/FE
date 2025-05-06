package com.example.maite.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.maite.MaiteRetrofitClient
import com.example.maite.model.InviteListItem
import com.example.maite.model.InviteRepository
import kotlinx.coroutines.launch
import android.app.Application
import androidx.lifecycle.AndroidViewModel

class InviteListViewModel(application: Application) : AndroidViewModel(application) {

    private val apiService = MaiteRetrofitClient.getInstance(application)
    private val repository = InviteRepository(apiService)

    private val _inviteList = MutableLiveData<List<InviteListItem>>()
    val inviteList: LiveData<List<InviteListItem>> = _inviteList

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadInviteList()
    }

    fun loadInviteList() {
        viewModelScope.launch {
            _error.value = null

            repository.getInviteList()
                .onSuccess { list ->
                    _inviteList.value = list
                }
                .onFailure { error ->
                    _error.value = "데이터 로드 실패: ${error.message}"
                }
        }
    }

    // Add a new invite to the list (이 기능은 서버와 연동 필요시 추가 수정 필요)
    fun addInvite(name: String) {
        // 실제 API 연동 필요시 구현
    }

    // Remove an invite from the list (이 기능은 서버와 연동 필요시 추가 수정 필요)
    fun removeInvite(inviteItem: InviteListItem) {
        // 실제 API 연동 필요시 구현
    }
}