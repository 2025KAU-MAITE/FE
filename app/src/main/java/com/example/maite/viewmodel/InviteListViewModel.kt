package com.example.maite.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.maite.model.InviteListItem
import com.example.maite.model.InviteRepository

class InviteListViewModel : ViewModel() {

    private val repository = InviteRepository()

    private val _inviteList = MutableLiveData<List<InviteListItem>>()
    val inviteList: LiveData<List<InviteListItem>> = _inviteList

    init {
        loadInviteList()
    }

    private fun loadInviteList() {
        // In a real app, this might be done in a background thread
        _inviteList.value = repository.getInviteList()
    }

    // Add a new invite to the list
    fun addInvite(name: String) {
        val newInvite = InviteListItem(name)
        val currentList = _inviteList.value?.toMutableList() ?: mutableListOf()
        currentList.add(newInvite)
        _inviteList.value = currentList
    }

    // Remove an invite from the list
    fun removeInvite(inviteItem: InviteListItem) {
        val currentList = _inviteList.value?.toMutableList() ?: mutableListOf()
        currentList.remove(inviteItem)
        _inviteList.value = currentList
    }
}