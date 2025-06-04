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
import com.example.maite.model.CreateMeetingRequest
import com.example.maite.model.CreatedMeetingResponse
import com.example.maite.model.SelectPlaceRequest
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.naver.maps.geometry.LatLng
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

class SuggestBottomSheet : BottomSheetDialogFragment(), PlaceBottomSheet.OnPlaceSelectedListener {
    private var _binding: BottomSheetSuggestBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: TimeSelectionViewModel by activityViewModels()
    private var availableDaysOfWeek: List<Int>? = null
    private var roomId: Int = -1 // newInstance를 통해 Int로 받음
    private var inviteEmails: List<String> = emptyList()

    private var selectedPlaceLatLng: LatLng? = null
    private var selectedPlaceName: String? = null

    // MaiteApiService 인스턴스 가져오기
    // MaiteRetrofitClient.getInstance(requireContext())가 MaiteApiService를 반환한다고 가정
    private val meetingApi: MaiteApiService by lazy {
        MaiteRetrofitClient.getInstance(requireContext())
    }

    companion object {
        const val ARG_AVAILABLE_DAYS = "available_days"
        const val ARG_ROOM_ID = "room_id"
        const val ARG_INVITE_EMAILS = "invite_emails"

        fun newInstance(
            availableDays: ArrayList<Int>,
            roomId: Int,
            inviteEmails: ArrayList<String>
        ): SuggestBottomSheet {
            val fragment = SuggestBottomSheet()
            fragment.arguments = Bundle().apply {
                putIntegerArrayList(ARG_AVAILABLE_DAYS, availableDays)
                putInt(ARG_ROOM_ID, roomId) // Int로 저장
                putStringArrayList(ARG_INVITE_EMAILS, inviteEmails)
            }
            Log.d("SuggestBottomSheet", "newInstance 호출됨, 전달된 요일: $availableDays, roomId: $roomId, inviteEmails: $inviteEmails")
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SuggestBottomSheet", "onCreate 호출됨")
        arguments?.let {
            availableDaysOfWeek = it.getIntegerArrayList(ARG_AVAILABLE_DAYS)
            roomId = it.getInt(ARG_ROOM_ID, -1) // Int로 읽음
            inviteEmails = it.getStringArrayList(ARG_INVITE_EMAILS) ?: emptyList()
            Log.d("SuggestBottomSheet", "onCreate에서 Argument 로드, 사용 가능 요일: $availableDaysOfWeek, roomId: $roomId, inviteEmails: $inviteEmails")
        }
        if (availableDaysOfWeek == null) {
            Log.w("SuggestBottomSheet", "사용 가능한 요일 정보가 전달되지 않았습니다. 모든 요일을 허용합니다.")
            availableDaysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7)
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

        setupObservers()
        setupListeners()
        checkAndUpdateDoneButtonState()
    }

    private fun setupListeners() {
        binding.titleEditText.addTextChangedListener {
            checkAndUpdateDoneButtonState()
        }

        binding.dateBtn.setOnClickListener {
            Log.d("SuggestBottomSheet", "dateBtn 클릭됨")
            availableDaysOfWeek?.let { days ->
                Log.d("SuggestBottomSheet", "DateBottomSheet 생성 시도, 전달 요일: $days")
                val datePicker = DateBottomSheet.newInstance(ArrayList(days))
                datePicker.show(childFragmentManager, "datePicker")
            } ?: run {
                Log.e("SuggestBottomSheet", "availableDaysOfWeek가 null이라 DateBottomSheet를 열 수 없습니다.")
                Toast.makeText(context, "요일 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.time1.setOnClickListener {
            if (sharedViewModel.getCurrentDate() == null) {
                Toast.makeText(context, "날짜를 먼저 선택해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentTime = parseTimeFromTextView(binding.time1Text)
            val timePicker = TimePickerBottomSheet.newInstance("time1", currentTime.first, currentTime.second)
            timePicker.show(childFragmentManager, "timePicker1")
        }

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
            // ViewModel에서 가져오는 값들이 nullable일 수 있으므로 안전 호출 또는 !! 사용
            val startTime = sharedViewModel.getCurrentStartTime()
            val endTime = sharedViewModel.getCurrentEndTime()
            val selectedDate = sharedViewModel.getCurrentDate()

            // 필수 값들이 null이 아닌지 확인
            if (title.isEmpty() || startTime == null || endTime == null || selectedDate == null || selectedPlaceName == null) {
                Toast.makeText(context, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                checkAndUpdateDoneButtonState() // 버튼 상태 다시 업데이트
                return@setOnClickListener
            }

            Log.i("SuggestBottomSheet", "회의 제안 완료 버튼 클릭: 제목='$title', 날짜=$selectedDate, 시작=$startTime, 종료=$endTime, 장소='$selectedPlaceName', 좌표=$selectedPlaceLatLng")
            sendMeetingProposal(title, selectedDate, startTime, endTime)
        }
    }

    private fun setupObservers() {
        Log.d("SuggestBottomSheet", "setupObservers 호출됨")
        sharedViewModel.startTime.observe(viewLifecycleOwner, Observer { startTimePair ->
            Log.d("SuggestBottomSheet", "시작 시간 LiveData 변경 감지: $startTimePair")
            updateTimeText(binding.time1Text, startTimePair)
            checkAndUpdateDoneButtonState()
        })

        sharedViewModel.endTime.observe(viewLifecycleOwner, Observer { endTimePair ->
            Log.d("SuggestBottomSheet", "종료 시간 LiveData 변경 감지: $endTimePair")
            updateTimeText(binding.time2Text, endTimePair)
            checkAndUpdateDoneButtonState()
        })

        sharedViewModel.selectedDate.observe(viewLifecycleOwner, Observer { date ->
            Log.d("SuggestBottomSheet", "날짜 LiveData 변경 감지: $date")
            updateDateText(date)
            checkAndUpdateDoneButtonState()
        })
    }

    private fun updateDateText(date: LocalDate?) {
        if (_binding == null) return
        binding.date.text = sharedViewModel.getFormattedDate() ?: "날짜 선택하기"
    }

    private fun updateTimeText(textView: TextView, timePair: TimePair?) {
        if (_binding == null) return
        textView.text = if (timePair != null) {
            String.format(Locale.getDefault(), "%02d : %02d", timePair.first, timePair.second)
        } else {
            "시간 선택"
        }
    }

    private fun checkAndUpdateDoneButtonState() {
        if (_binding == null) {
            Log.w("SuggestBottomSheet", "checkAndUpdateDoneButtonState 호출 시 바인딩이 null입니다.")
            return
        }
        val startTime = sharedViewModel.getCurrentStartTime()
        val endTime = sharedViewModel.getCurrentEndTime()
        val selectedDate = sharedViewModel.getCurrentDate()
        val isTitleEntered = binding.titleEditText.text.toString().trim().isNotEmpty()
        val isPlaceSelected = selectedPlaceLatLng != null // 또는 selectedPlaceName != null

        val isTimeValid = selectedDate != null && startTime != null && endTime != null &&
                sharedViewModel.isValidEndTime(endTime.first, endTime.second)
        val isOverallValid = isTitleEntered && isTimeValid && isPlaceSelected

        updateDoneButtonStateVisuals(isOverallValid)
    }

    private fun updateDoneButtonStateVisuals(isValid: Boolean) {
        if (_binding == null) return
        binding.doneBtn.isEnabled = isValid
        binding.doneBtn.isClickable = isValid
        val context = context ?: return

        val filterColor = ContextCompat.getColor(context, if (isValid) R.color.mainColor else R.color.btn_inactive)
        val textColor = ContextCompat.getColor(context, if (isValid) R.color.white else R.color.black)

        binding.btnBg.setColorFilter(filterColor)
        binding.btnText.setTextColor(textColor)
    }

    private fun parseTimeFromTextView(textView: TextView): Pair<Int, Int> {
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
            Log.e("SuggestBottomSheet", "시간 파싱 오류: '$timeString'", e)
            Pair(0, 0)
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
        selectedPlaceLatLng = latLng
        selectedPlaceName = name

        if (_binding != null) {
            binding.place.text = name ?: "위치: ${latLng.latitude}, ${latLng.longitude}"
        }
        checkAndUpdateDoneButtonState()
    }

    private fun sendMeetingProposal(
        title: String,
        selectedDate: LocalDate,
        startTime: TimePair,
        endTime: TimePair // 이제 이 값을 API 요청에서 사용합니다
    ) {
        if (roomId == -1) {
            Toast.makeText(context, "roomId가 설정되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (inviteEmails.isEmpty()) {
            Toast.makeText(context, "초대할 사용자가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        // selectedPlaceName이 null이면 함수를 더 진행하지 않도록 방어 코드 추가
        if (selectedPlaceName == null) {
            Toast.makeText(context, "장소를 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val meetingDateStr = selectedDate.toString() // "YYYY-MM-DD" 형식
        val meetingStartTimeStr = String.format(Locale.getDefault(), "%02d:%02d", startTime.first, startTime.second) // "HH:MM" 형식
        val meetingEndTimeStr = String.format(Locale.getDefault(), "%02d:%02d", endTime.first, endTime.second) // "HH:MM" 형식 - endTime 사용
        val currentAddress = selectedPlaceName!! // 위에서 null 체크 했으므로 !! 사용 가능

        if (_binding != null) {
            binding.doneBtn.isEnabled = false
            binding.btnText.text = "전송 중..."
        }

        lifecycleScope.launch {
            var createdMeetingId: Long? = null

            try {
                val createMeetingRequest = CreateMeetingRequest(
                    title = title,
                    meetingDate = meetingDateStr,
                    meetingTime = meetingStartTimeStr,
                    meetingEndTime = meetingEndTimeStr, // endTime 필드 추가
                    inviteEmails = inviteEmails
                )
                Log.d("SuggestBottomSheet", "1단계: 회의 생성 요청: roomId=$roomId, request=$createMeetingRequest")

                // MaiteApiService에 정의된 메소드 호출
                val createResponse = meetingApi.createMeetingInRoom(roomId.toLong(), createMeetingRequest)

                if (createResponse.isSuccessful && createResponse.body() != null) {
                    // API 응답에서 meetingId 추출
                    createdMeetingId = createResponse.body()!!.meetingId
                    Log.d("SuggestBottomSheet", "1단계: 회의 생성 성공, meetingId: $createdMeetingId")

                    // 회의 ID가 유효한지 확인 (0도 유효한 ID로 처리)
                    if (createdMeetingId != null) {
                        val selectPlaceRequest = SelectPlaceRequest(address = currentAddress)
                        Log.d("SuggestBottomSheet", "2단계: 장소 선택 요청: meetingId=$createdMeetingId, request=$selectPlaceRequest")
                        val selectPlaceResponse = meetingApi.selectMeetingPlace(createdMeetingId, selectPlaceRequest)

                        if (selectPlaceResponse.isSuccessful) {
                            Log.d("SuggestBottomSheet", "2단계: 장소 선택 성공")
                            Toast.makeText(context, "회의 제안을 성공적으로 보냈습니다!", Toast.LENGTH_SHORT).show()
                            dismiss()
                        } else {
                            val errorBody = selectPlaceResponse.errorBody()?.string() ?: selectPlaceResponse.message()
                            Log.e("SuggestBottomSheet", "2단계: 장소 선택 API 실패: ${selectPlaceResponse.code()} - $errorBody")
                            Toast.makeText(context, "회의는 생성되었으나 장소 지정에 실패했습니다: $errorBody", Toast.LENGTH_LONG).show()
                            dismiss()
                        }
                    } else {
                        // meetingId가 null인 경우는 발생하지 않아야 하지만, 방어적 코딩을 위해 유지
                        Log.e("SuggestBottomSheet", "1단계 성공했으나 meetingId가 null입니다. API 응답 확인 필요.")
                        Toast.makeText(context, "회의 생성 응답 오류 (meetingId 누락).", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorBody = createResponse.errorBody()?.string() ?: createResponse.message()
                    Log.e("SuggestBottomSheet", "1단계: 회의 생성 API 실패: ${createResponse.code()} - $errorBody")
                    Toast.makeText(context, "회의 제안 생성에 실패했습니다: $errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("SuggestBottomSheet", "회의 제안 API 호출 중 오류", e)
                Toast.makeText(context, "오류가 발생했습니다: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                if (_binding != null) {
                    binding.doneBtn.isEnabled = true
                    binding.btnText.text = "제안하기"
                }
            }
        }
    }
}