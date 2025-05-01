package com.example.maite

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener // EditText 변경 감지
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.maite.databinding.BottomSheetSuggestBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.naver.maps.geometry.LatLng
import java.time.LocalDate
import java.util.Locale
// Import FragmentTransaction if needed, though usually included with fragment imports
// import androidx.fragment.app.FragmentTransaction

class SuggestBottomSheet : BottomSheetDialogFragment(), PlaceBottomSheet.OnPlaceSelectedListener {
    private var _binding: BottomSheetSuggestBinding? = null
    private val binding get() = _binding!!

    // ViewModel 공유 (Activity 스코프) - 실제 ViewModel 클래스로 교체 필요
    private val sharedViewModel: TimeSelectionViewModel by activityViewModels()
    private var availableDaysOfWeek: List<Int>? = null

    private var selectedPlaceLatLng: LatLng? = null
    private var selectedPlaceName: String? = null

    companion object {
        const val ARG_AVAILABLE_DAYS = "available_days"

        // newInstance 수정: 사용 가능한 요일 목록을 받도록 함
        fun newInstance(availableDays: ArrayList<Int>): SuggestBottomSheet {
            val fragment = SuggestBottomSheet()
            fragment.arguments = Bundle().apply {
                putIntegerArrayList(ARG_AVAILABLE_DAYS, availableDays)
            }
            Log.d("SuggestBottomSheet", "newInstance 호출됨, 전달된 요일: $availableDays")
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("SuggestBottomSheet", "onCreate 호출됨")
        // Argument에서 사용 가능한 요일 목록 가져오기
        arguments?.let {
            availableDaysOfWeek = it.getIntegerArrayList(ARG_AVAILABLE_DAYS)
            Log.d("SuggestBottomSheet", "onCreate에서 Argument 로드, 사용 가능 요일: $availableDaysOfWeek")
        }
        if (availableDaysOfWeek == null) {
            Log.w("SuggestBottomSheet", "사용 가능한 요일 정보가 전달되지 않았습니다. 모든 요일을 허용합니다.")
            availableDaysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7) // 기본값: 모든 요일 허용
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

            // 여기서 api 통해 정보 전달

            Toast.makeText(context, "회의 제안이 완료되었습니다", Toast.LENGTH_SHORT).show()

            dismiss()
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
}