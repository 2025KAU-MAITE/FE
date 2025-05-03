package com.example.maite.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.maite.model.TimetableEntry

object TimetableStore {
    private val _entries = MutableLiveData<List<TimetableEntry>>(emptyList())
    val entries: LiveData<List<TimetableEntry>> get() = _entries

    fun set(entries: List<TimetableEntry>) {
        _entries.value = entries
    }

    fun add(entry: TimetableEntry) {
        val current = _entries.value?.toMutableList() ?: mutableListOf()
        current.add(entry)
        _entries.value = current
    }

    fun clear() {
        _entries.value = emptyList()
    }

    fun remove(entry: TimetableEntry) {
        val current = _entries.value?.toMutableList() ?: mutableListOf()
        current.remove(entry)
        _entries.value = current
    }
}
