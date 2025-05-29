package com.example.maite

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.maite.databinding.BottomSheetSuggestBinding
import com.example.maite.ApiClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.naver.maps.geometry.LatLng
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

class SuggestBottomSheet : BottomSheetDialogFragment(), PlaceBottomSheet.OnPlaceSelectedListener {
    private var _binding: BottomSheetSuggestBinding? = null
    private val binding get() = _binding!!

    // ViewModel 공유 (Activity 스코프) - 실제 ViewModel 클래스로 교체 필요
    private val sharedViewModel: TimeSelectionViewModel by activityViewModels()
    private var availableDaysOfWeek: List<Int>? = null
    private var roomId: Int = -1
    private var inviteEmails: List<String> = emptyList()

    private var selectedPlaceLatLng: LatLng? = null
    private var selectedPlaceName: String? = null
    
    private val meetingApi by lazy {
        ApiClient.getClient(requireContext()).create(MeetingApi::class.java)
    }

    companion object {
        const val ARG_AVAILABLE_DAYS = "available_days"
        const val ARG_ROOM_ID = "room_id"
        const val ARG_INVITE_EMAILS = "invite_emails"

        // newInstance 수정: 사용 가능한 요일 목록과 roomId, inviteEmails를 받도록 함
        fun newInstance(
            availableDays: ArrayList<Int>, 
            roomId: Int, 
            inviteEmails: ArrayList<String>
        ): SuggestBottomSheet {
            val fragment = SuggestBottomSheet()
            fragment.arguments = Bundle().apply {
                putIntegerArrayList(ARG_AVAILABLE_DAYS, availableDays)
                putInt(ARG_ROOM_ID, roomId)
                putStringArrayList(ARG_INVITE_EMAILS, inviteEmails)
            }
            Log.d("SuggestBottomSheet", "newInstance 호출됨, 전달된 요일: $availableDays, roomId: $roomId, inviteEmails: $inviteEmails")
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SuggestBottomSheet", "onCreate 호출됨")
        // Argument에서 데이터 가져오기
        arguments?.let {
            availableDaysOfWeek = it.getIntegerArrayList(ARG_AVAILABLE_DAYS)
            roomId = it.getInt(ARG_ROOM_ID, -1)
            inviteEmails = it.getStringArrayList(ARG_INVITE_EMAILS) ?: emptyList()
            Log.d("SuggestBottomSheet", "onCreate에서 Argument 로드, 사용 가능 요일: $availableDaysOfWeek, roomId: $roomId, inviteEmails: $inviteEmails")
        }
        if (availableDaysOfWeek == null) {
            Log.w("SuggestBottomSheet", "사용 가능한 요일 정보가 전달되지 않았습니다. 모든 요일을 허용합니다.")
            availableDaysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7) // 기본값: 모든 요일 허용
        }
        if (roomId == -1) {
            Log.e("SuggestBottomSheet", "roomId가 전달되지 않았습니다!")
        }
        if (inviteEmails.isEmpty()) {
            Log.w("SuggestBottomSheet", "inviteEmails가 비어있습니다. 아무에게도 알림이 가지 않습니다.")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("SuggestBottomSheet", "onCreateView 호출됨")
        _binding = BottomSheetSuggestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("SuggestBottomSheet", "onViewCreated 호출됨")

        setupObservers() // LiveData 관찰 설정
        setupListeners() // 리스너 설정 분리
        checkAndUpdateDoneButtonState() // 초기 버튼 상태 확인
    }

    // 리스너 설정 함수
    private fun setupListeners() {
        // 제목 EditText 텍스트 변경 리스너
        binding.titleEditText.addTextChangedListener {
            checkAndUpdateDoneButtonState() // 텍스트 변경 시 완료 버튼 상태 재확인
        }

        // dateBtn 클릭 시 DateBottomSheet 표시
        binding.dateBtn.setOnClickListener {
            Log.d("SuggestBottomSheet", "dateBtn 클릭됨")
            availableDaysOfWeek?.let { days ->
                Log.d("SuggestBottomSheet", "DateBottomSheet 생성 시도, 전달 요일: $days")
                // 실제 DateBottomSheet 클래스로 교체 필요
                val datePicker = DateBottomSheet.newInstance(ArrayList(days))
                datePicker.show(childFragmentManager, "datePicker")
            } ?: run {
                Log.e("SuggestBottomSheet", "availableDaysOfWeek가 null이라 DateBottomSheet를 열 수 없습니다.")
                Toast.makeText(context, "요일 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // time1 버튼 리스너
        binding.time1.setOnClickListener {
            if (sharedViewModel.getCurrentDate() == null) {
                Toast.makeText(context, "날짜를 먼저 선택해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentTime = parseTimeFromTextView(binding.time1Text)
            // 실제 TimePickerBottomSheet 클래스로 교체 필요
            val timePicker = TimePickerBottomSheet.newInstance("time1", currentTime.first, currentTime.second)
            timePicker.show(childFragmentManager, "timePicker1")
        }

        // time2 버튼 리스너
        binding.time2.setOnClickListener {
            if (sharedViewModel.getCurrentDate() == null) {
                Toast.makeText(context, "날짜를 먼저 선택해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (sharedViewModel.getCurrentStartTime() == null) {
                Toast.makeText(context, "시작 시간을 먼저 선택해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentTime = parseTimeFromTextView(binding.time2Text)
            // 실제 TimePickerBottomSheet 클래스로 교체 필요
            val timePicker = TimePickerBottomSheet.newInstance("time2", currentTime.first, currentTime.second)
            timePicker.show(childFragmentManager, "timePicker2")
        }

        binding.placeCardView.setOnClickListener {
            val placeBottomSheet = PlaceBottomSheet.newInstance()
            placeBottomSheet.setOnPlaceSelectedListener(this)
            placeBottomSheet.show(parentFragmentManager, placeBottomSheet.tag)
        }

        binding.doneBtn.setOnClickListener {
            if (!binding.doneBtn.isEnabled) {
                return@setOnClickListener
            }

            val title = binding.titleEditText.text.toString().trim()
            val startTime = sharedViewModel.getCurrentStartTime()!!
            val endTime = sharedViewModel.getCurrentEndTime()!!
            val selectedDate = sharedViewModel.getCurrentDate()!!
            val placeName = selectedPlaceName ?: "선택된 장소 없음"
            val placeLatLng = selectedPlaceLatLng

            Log.i("SuggestBottomSheet", "회의 제안 완료: 제목='$title', 날짜=$selectedDate, 시작=$startTime, 종료=$endTime, 장소='$placeName', 좌표=$placeLatLng")

            // 회의 제안 API 호출
            sendMeetingProposal(title, selectedDate, startTime, endTime)
        }
    }


    // LiveData 관찰 설정
    private fun setupObservers() {
        Log.d("SuggestBottomSheet", "setupObservers 호출됨")

        // 시작 시간 관찰
        sharedViewModel.startTime.observe(viewLifecycleOwner, Observer { startTimePair ->
            Log.d("SuggestBottomSheet", "시작 시간 LiveData 변경 감지: $startTimePair")
            updateTimeText(binding.time1Text, startTimePair)
            checkAndUpdateDoneButtonState()
        })

        // 종료 시간 관찰
        sharedViewModel.endTime.observe(viewLifecycleOwner, Observer { endTimePair ->
            Log.d("SuggestBottomSheet", "종료 시간 LiveData 변경 감지: $endTimePair")
            updateTimeText(binding.time2Text, endTimePair)
            checkAndUpdateDoneButtonState()
        })

        // 날짜 관찰
        sharedViewModel.selectedDate.observe(viewLifecycleOwner, Observer { date ->
            Log.d("SuggestBottomSheet", "날짜 LiveData 변경 감지: $date")
            updateDateText(date)
            checkAndUpdateDoneButtonState()
        })
    }

    // 날짜 텍스트 업데이트
    private fun updateDateText(date: LocalDate?) {
        if (_binding == null) return
        // ViewModel의 포맷 함수 사용 (실제 함수명으로 교체 필요)
        binding.date.text = sharedViewModel.getFormattedDate() ?: "날짜 선택하기"
    }

    // 시간 텍스트 업데이트
    private fun updateTimeText(textView: TextView, timePair: TimePair?) {
        if (_binding == null) return
        textView.text = if (timePair != null) {
            // Locale.getDefault() 사용 또는 특정 로케일 지정
            String.format(Locale.getDefault(), "%02d : %02d", timePair.first, timePair.second)
        } else {
            // 초기값 또는 null일 때 표시할 텍스트
            // time1Text와 time2Text ID를 비교하여 다른 초기 텍스트 설정 가능
            "시간 선택" // 예시: 양쪽 모두 "시간 선택"으로 표시
            // if (textView.id == binding.time1Text.id) "시작 시간" else "종료 시간" // 다른 예시
        }
    }

    // 완료 버튼 상태 업데이트 조건 확인
    private fun checkAndUpdateDoneButtonState() {
        if (_binding == null) {
            Log.w("SuggestBottomSheet", "checkAndUpdateDoneButtonState 호출 시 바인딩이 null입니다.")
            return
        }
        val startTime = sharedViewModel.getCurrentStartTime()
        val endTime = sharedViewModel.getCurrentEndTime()
        val selectedDate = sharedViewModel.getCurrentDate()
        val isTitleEntered = binding.titleEditText.text.toString().trim().isNotEmpty()
        val isPlaceSelected = selectedPlaceLatLng != null

        val isTimeValid = selectedDate != null && startTime != null && endTime != null &&
                sharedViewModel.isValidEndTime(endTime.first, endTime.second)
        val isOverallValid = isTitleEntered && isTimeValid && isPlaceSelected

        updateDoneButtonStateVisuals(isOverallValid) // 시각적 업데이트 함수 호출
    }

    // 완료 버튼 시각/활성화 상태 업데이트
    private fun updateDoneButtonStateVisuals(isValid: Boolean) {
        if (_binding == null) return
        binding.doneBtn.isEnabled = isValid
        binding.doneBtn.isClickable = isValid
        val context = context ?: return // context null 체크

        // 실제 색상 리소스로 교체 필요 (e.g., R.color.mainColor, R.color.btn_inactive 등)
        val filterColor = ContextCompat.getColor(context, if (isValid) R.color.mainColor else R.color.btn_inactive)
        val textColor = ContextCompat.getColor(context, if (isValid) R.color.white else R.color.black) // 비활성 시 텍스트 색상

        binding.btnBg.setColorFilter(filterColor)
        binding.btnText.setTextColor(textColor)
    }

    // TextView에서 시간 파싱 (오류 처리 강화)
    private fun parseTimeFromTextView(textView: TextView): Pair<Int, Int> {
        val timeString = textView.text.toString()
        // 초기 텍스트("시간 선택" 등) 또는 잘못된 형식 처리
        if (!timeString.contains(":")) return Pair(0, 0) // 기본값 또는 적절한 초기값 반환

        return try {
            val parts = timeString.split(":")
            if (parts.size == 2) {
                val hour = parts[0].trim().toIntOrNull()?.coerceIn(0, 23) ?: 0
                val minute = parts[1].trim().toIntOrNull()?.coerceIn(0, 59) ?: 0
                Pair(hour, minute)
            } else {
                Pair(0, 0) // 형식 안 맞으면 기본값 반환
            }
        } catch (e: Exception) {
            Log.e("SuggestBottomSheet", "시간 파싱 오류: '$timeString'", e)
            Pair(0, 0) // 오류 시 기본값 반환
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("SuggestBottomSheet", "onDestroyView 호출됨")
        selectedPlaceLatLng = null
        selectedPlaceName = null
        _binding = null
    }

    override fun onPlaceSelected(latLng: LatLng, name: String?) {
        Log.d("SuggestBottomSheet", "onPlaceSelected 호출됨: 좌표=$latLng, 이름=$name")
        // 선택된 장소 정보 저장
        selectedPlaceLatLng = latLng
        selectedPlaceName = name

        if (_binding != null) {
            binding.place.text = name ?: "위치: ${latLng.latitude}, ${latLng.longitude}"
        }

        checkAndUpdateDoneButtonState()
    }
    
    // 회의 제안 API 호출 메서드
    private fun sendMeetingProposal(
        title: String,
        selectedDate: LocalDate,
        startTime: TimePair,
        endTime: TimePair
    ) {
        if (roomId == -1) {
            Toast.makeText(context, "roomId가 설정되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (inviteEmails.isEmpty()) {
            Toast.makeText(context, "초대할 사용자가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 날짜와 시간을 API 형식으로 변환
        val meetingDate = selectedDate.toString() // "2025-05-29" 형식
        val meetingTime = String.format("%02d:%02d", startTime.first, startTime.second) // "14:00" 형식
        val address = selectedPlaceName ?: "선택된 장소 없음" // 선택된 장소 이름
        
        val request = MeetingProposalRequest(
            title = title,
            meetingDate = meetingDate,
            meetingTime = meetingTime,
            inviteEmails = inviteEmails,
            address = address  // 🆕 장소 정보 추가
        )
        
        Log.d("SuggestBottomSheet", "API 호출 시작: roomId=$roomId, request=$request")
        
        // 로딩 상태 표시 (선택사항)
        binding.doneBtn.isEnabled = false
        binding.btnText.text = "전송 중..."
        
        lifecycleScope.launch {
            try {
                val response = meetingApi.sendMeetingProposal(roomId, request)
                
                if (response.isSuccessful) {
                    Log.d("SuggestBottomSheet", "API 호출 성공: ${response.code()}")
                    Toast.makeText(context, "회의 제안을 보냈습니다!", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    Log.e("SuggestBottomSheet", "API 호출 실패: ${response.code()} - ${response.message()}")
                    Toast.makeText(context, "회의 제안 전송에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("SuggestBottomSheet", "회의 제안 API 호출 중 오류", e)
                Toast.makeText(context, "네트워크 오류가 발생했습니다. 다시 시도해주세요.", Toast.LENGTH_LONG).show()
            } finally {
                // 로딩 상태 해제
                if (_binding != null) {
                    binding.doneBtn.isEnabled = true
                    binding.btnText.text = "제안하기"
                }
            }
        }
    }
}