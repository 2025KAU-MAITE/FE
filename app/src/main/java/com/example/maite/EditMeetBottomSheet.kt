package com.example.maite

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.example.maite.databinding.BottomSheetEditMeetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale
import androidx.fragment.app.activityViewModels
import java.time.DayOfWeek

// ARG_MEETING_ID를 충돌 없이 처리하기 위한 상수 이름 변경
private const val ARG_EDIT_MEETING_ID = "edit_meeting_id"

class EditMeetBottomSheet : BottomSheetDialogFragment(), PlaceBottomSheet.OnPlaceSelectedListener {

    private var _binding: BottomSheetEditMeetBinding? = null
    private val binding get() = _binding!!

    private lateinit var apiService: MaiteApiService
    private var meetingId: Long = -1

    // 현재 선택된 장소를 저장할 변수
    private var selectedPlace: String? = null

    private val sharedViewModel: TimeSelectionViewModel by activityViewModels()

    // 결과 전달을 위한 키
    companion object {
        const val TAG = "EditMeetBottomSheet"
        const val REQUEST_KEY = "edit_meeting_result"
        const val BUNDLE_KEY_SUCCESS = "edit_success"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            meetingId = it.getLong(ARG_EDIT_MEETING_ID, -1)
        }
        apiService = MaiteRetrofitClient.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEditMeetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 초기 데이터 설정
        arguments?.let {
            binding.titleEditText.setText(it.getString(ARG_CURRENT_TITLE))

            // 날짜 설정
            val meetingDate = it.getString(ARG_CURRENT_DATE, "")
            if (meetingDate.isNotEmpty()) {
                // UI에 날짜 표시
                binding.date.text = formatDateWithDayOfWeek(meetingDate)

                // ViewModel에 날짜 설정 (추가된 부분)
                try {
                    val parts = meetingDate.split("-")
                    if (parts.size >= 3) {
                        val year = parts[0].toInt()
                        val month = parts[1].toInt()
                        val day = parts[2].toInt()
                        val localDate = LocalDate.of(year, month, day)
                        sharedViewModel.updateSelectedDate(localDate)
                        Log.d(TAG, "초기 날짜를 ViewModel에 설정: $localDate")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "초기 날짜를 ViewModel에 설정 실패: $meetingDate", e)
                }
            }

            selectedPlace = it.getString(ARG_CURRENT_PLACE)
            binding.place.text = selectedPlace ?: "장소를 선택해주세요"

            // 시간 데이터 파싱 및 설정
            val timeString = it.getString(ARG_CURRENT_TIME)
            if (timeString != null && timeString.contains("~")) {
                val times = timeString.split("~").map { t -> t.trim() }
                if (times.size == 2) {
                    // UI에 시간 표시
                    binding.time1Text.text = times[0].replace(":", " : ")
                    binding.time2Text.text = times[1].replace(":", " : ")

                    // ViewModel에 시간 설정 (추가된 부분)
                    try {
                        // 시작 시간 파싱
                        val startTimeParts = times[0].split(":")
                        if (startTimeParts.size == 2) {
                            val startHour = startTimeParts[0].trim().toInt()
                            val startMinute = startTimeParts[1].trim().toInt()
                            sharedViewModel.updateStartTime(startHour, startMinute)
                            Log.d(TAG, "초기 시작 시간을 ViewModel에 설정: $startHour:$startMinute")
                        }

                        // 종료 시간 파싱
                        val endTimeParts = times[1].split(":")
                        if (endTimeParts.size == 2) {
                            val endHour = endTimeParts[0].trim().toInt()
                            val endMinute = endTimeParts[1].trim().toInt()
                            sharedViewModel.updateEndTime(endHour, endMinute)
                            Log.d(TAG, "초기 종료 시간을 ViewModel에 설정: $endHour:$endMinute")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "초기 시간을 ViewModel에 설정 실패: $timeString", e)
                    }
                } else {
                    binding.time1Text.text = "00 : 00"
                    binding.time2Text.text = "00 : 00"
                }
            } else {
                binding.time1Text.text = "00 : 00"
                binding.time2Text.text = "00 : 00"
            }
        }

        // 리스너 설정
        binding.dateBtn.setOnClickListener {
            // 현재 표시된 날짜 텍스트 가져오기
            val currentDateText = binding.date.text.toString()

            // 날짜 형식이 "YYYY.MM.DD.요일"인 경우 파싱
            try {
                // 정규식을 사용하여 YYYY.MM.DD 부분만 추출
                val regex = "(\\d{4})\\.(\\d{2})\\.(\\d{2}).*".toRegex()
                val matchResult = regex.find(currentDateText)

                if (matchResult != null) {
                    val (yearStr, monthStr, dayStr) = matchResult.destructured
                    val year = yearStr.toInt()
                    val month = monthStr.toInt()
                    val day = dayStr.toInt()

                    // LocalDate 객체 생성
                    val localDate = LocalDate.of(year, month, day)

                    // ViewModel에 날짜 설정
                    sharedViewModel.updateSelectedDate(localDate)
                    Log.d(TAG, "DateBottomSheet 열기 전 ViewModel 날짜 설정: $localDate")
                }
            } catch (e: Exception) {
                Log.e(TAG, "날짜 파싱 오류: $currentDateText", e)
            }

            // DateBottomSheet 열기
            val datePicker = DateBottomSheet.newInstance(ArrayList(listOf(1, 2, 3, 4, 5, 6, 7))) // 모든 요일 허용
            datePicker.show(childFragmentManager, "datePicker")
        }

        binding.time1.setOnClickListener {
            val currentTime = parseTimeFromTextView(binding.time1Text)
            val timePicker = TimePickerBottomSheet.newInstance("time1", currentTime.first, currentTime.second)
            timePicker.show(childFragmentManager, "timePicker1")
        }

        binding.time2.setOnClickListener {
            val currentTime = parseTimeFromTextView(binding.time2Text)
            val timePicker = TimePickerBottomSheet.newInstance("time2", currentTime.first, currentTime.second)
            timePicker.show(childFragmentManager, "timePicker2")
        }

        binding.placeCardView.setOnClickListener {
            // meetingId와 함께 AI 버튼 활성화
            val placeBottomSheet = PlaceBottomSheet.newInstance(true, meetingId)
            placeBottomSheet.setOnPlaceSelectedListener(this)
            placeBottomSheet.show(parentFragmentManager, placeBottomSheet.tag)
        }

        binding.doneBtn.setOnClickListener {
            updateMeetingInfo()
        }

        // DateBottomSheet에서 선택한 날짜 처리
        childFragmentManager.setFragmentResultListener("date_selection", viewLifecycleOwner) { _, bundle ->
            val selectedDate = bundle.getString("selected_date")
            if (!selectedDate.isNullOrEmpty()) {
                binding.date.text = selectedDate
            }
        }

        // TimePickerBottomSheet에서 선택한 시간 처리
        childFragmentManager.setFragmentResultListener("time_selection", viewLifecycleOwner) { _, bundle ->
            val timeKey = bundle.getString("time_key")
            val hour = bundle.getInt("hour", 0)
            val minute = bundle.getInt("minute", 0)
            val formattedTime = String.format(Locale.getDefault(), "%02d : %02d", hour, minute)

            when (timeKey) {
                "time1" -> binding.time1Text.text = formattedTime
                "time2" -> binding.time2Text.text = formattedTime
            }
        }

        setupViewModelObservers()
    }

    private fun setupViewModelObservers() {
        sharedViewModel.startTime.observe(viewLifecycleOwner) { startTimePair ->
            if (startTimePair != null) {
                binding.time1Text.text = String.format(Locale.getDefault(), "%02d : %02d",
                    startTimePair.first, startTimePair.second)
                Log.d(TAG, "ViewModel 시작 시간 변경 감지: ${startTimePair.first}:${startTimePair.second}")
            }
        }

        sharedViewModel.endTime.observe(viewLifecycleOwner) { endTimePair ->
            if (endTimePair != null) {
                binding.time2Text.text = String.format(Locale.getDefault(), "%02d : %02d",
                    endTimePair.first, endTimePair.second)
                Log.d(TAG, "ViewModel 종료 시간 변경 감지: ${endTimePair.first}:${endTimePair.second}")
            }
        }

        sharedViewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            if (date != null) {
                // 날짜 포맷팅 함수를 사용하여 요일 정보 포함한 날짜 표시
                val formattedDate = formatDateWithDayOfWeek(date.toString())
                binding.date.text = formattedDate
                Log.d(TAG, "ViewModel 날짜 변경 감지: $date -> $formattedDate")
            }
        }
    }

    private fun updateMeetingInfo() {
        if (meetingId == -1L) {
            Toast.makeText(context, "회의 ID가 유효하지 않습니다", Toast.LENGTH_SHORT).show()
            return
        }

        val title = binding.titleEditText.text.toString().trim()
        val meetingDay = binding.date.text.toString().trim()
        val startTime = parseTimeFromTextView(binding.time1Text)
        val endTime = parseTimeFromTextView(binding.time2Text)

        if (title.isEmpty() || meetingDay == "날짜 선택하기") {
            Toast.makeText(context, "제목과 날짜를 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPlace.isNullOrEmpty()) {
            Toast.makeText(context, "장소를 선택해주세요", Toast.LENGTH_SHORT).show()
            return
        }

        val startTimeStr = String.format(Locale.getDefault(), "%02d:%02d", startTime.first, startTime.second)
        val endTimeStr = String.format(Locale.getDefault(), "%02d:%02d", endTime.first, endTime.second)

        // 시작 시간이 종료 시간보다 늦을 경우 검증
        if (!isValidTimeRange(startTime, endTime)) {
            Toast.makeText(context, "종료 시간은 시작 시간보다 늦어야 합니다", Toast.LENGTH_SHORT).show()
            return
        }

        // 회의 업데이트 API 호출
        lifecycleScope.launch {
            try {
                // 버튼 상태 업데이트
                binding.btnText.text = "업데이트 중..."
                binding.doneBtn.isClickable = false
                binding.btnBg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.btn_inactive))

                val request = UpdateMeetingRequest(
                    title = title,
                    meetingDay = formatDateForServer(meetingDay), // 서버 형식에 맞게 변환
                    meetingTime = startTimeStr,
                    meetingEndTime = endTimeStr,
                    address = selectedPlace!!
                )

                val response = withContext(Dispatchers.IO) {
                    apiService.updateMeeting(meetingId, request)
                }

                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        // 부모 Fragment에게 업데이트 완료 알림
                        val resultBundle = Bundle().apply {
                            putBoolean(BUNDLE_KEY_SUCCESS, true)
                            putLong("updated_meeting_id", meetingId)
                        }
                        setFragmentResult(REQUEST_KEY, resultBundle)


                        dismiss()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val errorMsg = response.errorBody()?.string() ?: "업데이트 실패"
                        Toast.makeText(context, "오류: $errorMsg", Toast.LENGTH_SHORT).show()

                        // 버튼 상태 복원
                        binding.btnText.text = "수정하기"
                        binding.doneBtn.isClickable = true
                        binding.btnBg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.mainColor))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_SHORT).show()

                    // 버튼 상태 복원
                    binding.btnText.text = "수정하기"
                    binding.doneBtn.isClickable = true
                    binding.btnBg.setColorFilter(ContextCompat.getColor(requireContext(), R.color.mainColor))
                }
            }
        }
    }

    private fun formatDateForServer(displayDate: String): String {
        // "2025.06.10.화요일" → "2025-06-10" 형식으로 변환
        val regex = "(\\d{4})\\.(\\d{2})\\.(\\d{2})".toRegex()
        val matchResult = regex.find(displayDate)

        return if (matchResult != null) {
            val (year, month, day) = matchResult.destructured
            "$year-$month-$day"
        } else {
            // 정규식으로 추출 실패 시 기본 변환 시도
            val parts = displayDate.split(".")
            if (parts.size >= 3) {
                // 처음 세 부분만 가져와서 '-'로 연결
                "${parts[0]}-${parts[1]}-${parts[2]}"
            } else {
                // 그 외 경우에는 단순히 점을 대시로 변환 (위험할 수 있음)
                displayDate.replace(".", "-")
            }
        }
    }

    private fun formatDateWithDayOfWeek(dateString: String): String {
        try {
            // 하이픈(-)으로 구분된 날짜 문자열을 파싱
            val parts = dateString.split("-")
            if (parts.size != 3) return dateString.replace("-", ".")

            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()

            // LocalDate 객체 생성
            val date = LocalDate.of(year, month, day)

            // 요일 계산 (DayOfWeek 열거형 값을 얻음)
            val dayOfWeek = date.dayOfWeek

            // 요일을 한글로 변환
            val koreanDayOfWeek = when (dayOfWeek) {
                DayOfWeek.MONDAY -> "월요일"
                DayOfWeek.TUESDAY -> "화요일"
                DayOfWeek.WEDNESDAY -> "수요일"
                DayOfWeek.THURSDAY -> "목요일"
                DayOfWeek.FRIDAY -> "금요일"
                DayOfWeek.SATURDAY -> "토요일"
                DayOfWeek.SUNDAY -> "일요일"
                else -> ""
            }

            // 최종 형식 생성: 2025.06.09.월요일
            return String.format(Locale.getDefault(), "%04d.%02d.%02d.%s",
                year, month, day, koreanDayOfWeek)
        } catch (e: Exception) {
            Log.e(TAG, "날짜 형식 변환 오류: $dateString", e)
            // 오류 발생 시 기본 변환만 적용
            return dateString.replace("-", ".")
        }
    }

    private fun parseTimeFromTextView(textView: android.widget.TextView): Pair<Int, Int> {
        val timeString = textView.text.toString()
        if (!timeString.contains(":")) return Pair(0, 0)

        return try {
            val parts = timeString.split(":")
            if (parts.size == 2) {
                val hour = parts[0].trim().toIntOrNull()?.coerceIn(0, 23) ?: 0
                val minute = parts[1].trim().toIntOrNull()?.coerceIn(0, 59) ?: 0
                Pair(hour, minute)
            } else {
                Pair(0, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "시간 파싱 오류: '$timeString'", e)
            Pair(0, 0)
        }
    }

    private fun isValidTimeRange(startTime: Pair<Int, Int>, endTime: Pair<Int, Int>): Boolean {
        // 시작 시간이 종료 시간보다 이전인지 확인
        return when {
            startTime.first < endTime.first -> true
            startTime.first == endTime.first -> startTime.second < endTime.second
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPlaceSelected(latLng: com.naver.maps.geometry.LatLng, name: String?) {
        selectedPlace = name ?: "${latLng.latitude}, ${latLng.longitude}"
        binding.place.text = selectedPlace
    }
}