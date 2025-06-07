package com.example.maite.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.maite.model.MeetListItem
import com.example.maite.model.MeetListRepository
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeetListViewModel : ViewModel() {
    // 싱글톤 인스턴스 사용
    private val repository = MeetListRepository.getInstance()

    private val _meetList = MutableLiveData<List<MeetListItem>>()
    val meetList: LiveData<List<MeetListItem>> = _meetList

    // 날짜 형식 파서 (yyyy-MM-dd)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        loadMeetList()
    }

    fun loadMeetList() {
        // Repository에서 데이터 로드
        val data = repository.getMeetList()

        // 날짜 기준으로 내림차순 정렬 (가장 최근 날짜가 맨 위에, 가장 빠른 날짜가 맨 밑에)
        val sortedData = data.sortedByDescending { meetItem ->
            try {
                // 날짜 파싱
                dateFormat.parse(meetItem.date) ?: Date(0) // 파싱 실패시 가장 오래된 날짜로 설정
            } catch (e: Exception) {
                Log.e("MeetListViewModel", "날짜 파싱 오류: ${meetItem.date}", e)
                // 파싱 오류 시 가장 오래된 날짜로 처리
                Date(0)
            }
        }

        Log.d("MeetListViewModel", "회의 정렬 완료 (내림차순): ${sortedData.size}개 항목")
        sortedData.forEachIndexed { index, item ->
            Log.d("MeetListViewModel", "$index: ${item.title}, 날짜: ${item.date}")
        }

        _meetList.value = sortedData
        Log.d("MeetListViewModel", "회의 목록 로드: ${sortedData.size}개 항목")
    }

    fun refreshData() {
        // 저장소에서 최신 데이터 가져오기
        val latestData = MeetListRepository.getInstance().getMeetList()

        // LiveData 업데이트
        _meetList.value = latestData
    }
}