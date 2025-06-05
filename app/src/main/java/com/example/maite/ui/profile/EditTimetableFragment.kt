package com.example.maite.ui.profile

import android.graphics.Color
import android.os.Bundle

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.core.widget.addTextChangedListener
import com.example.maite.R
import com.example.maite.databinding.FragmentEditTimetableBinding
import com.example.maite.model.TimetableEntry
import com.example.maite.ui.profile.ProfileViewModel
import com.example.maite.ui.profile.EditTimeSelectionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.ceil
import com.example.maite.PreferencesUtil
import com.example.maite.ui.profile.LoadingDialog
import com.example.maite.util.TimetableColorManager

class EditTimetableFragment : Fragment() {

    private var _binding: FragmentEditTimetableBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by activityViewModels()

    // 독립적인 EditTimeSelectionViewModel 사용
    private val timeSelectionViewModel: EditTimeSelectionViewModel by activityViewModels()
    
    // 색상 관리자
    private lateinit var colorManager: TimetableColorManager

    // 요일 선택 옵션
    private val dayOptions = arrayOf("월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일")

    // 현재 선택된 요일 인덱스 (0: 월요일)
    private var selectedDayIndex = 0

    // 변경사항 추적 플래그
    private var hasChanges = false

    // 현재 선택된 시작/종료 시간 (시간:분)
    private var selectedStartHour = 9
    private var selectedStartMinute = 0
    private var selectedEndHour = 10
    private var selectedEndMinute = 0

    // 선택된 시간표 항목 (삭제 기능용)
    private var selectedEntry: TimetableEntry? = null

    // 색상은 일관되게 유지
    private val defaultColor = "#4C7EED"

    // 임시 시간표 리스트 (서버 데이터와 동기화)
    private val temporaryEntries = mutableListOf<TimetableEntry>()

    // 삭제 모드 플래그
    private var isDeleteMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditTimetableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 색상 관리자 초기화
        colorManager = TimetableColorManager(requireContext())

        // 서버에서 최신 시간표 데이터 로드
        val preferencesUtil = PreferencesUtil(requireContext())
        val userId = preferencesUtil.getUserId()
        
        if (userId != null) {
            lifecycleScope.launch {
                viewModel.loadTimetableFromServer(userId)
            }
        }
        
        // 기존 시간표 항목들을 임시 리스트에 복사 (서버 데이터 로드 후에 업데이트됨)
        viewModel.timetable.value?.let {
            temporaryEntries.clear()
            temporaryEntries.addAll(it)
        }
        
        // 초기 상태에서는 변경사항 없음으로 설정
        hasChanges = false

        setupUI()
        setupTimeSelectionObservers()
        observeEvents()

        // 초기 시간표 미리보기 표시
        updateTimetablePreview()
        
        // 초기 버튼 상태 설정
        updateSaveButtonState()
    }

    private fun setupUI() {
        // 요일 선택 버튼 설정
        binding.btnDaySelect.text = dayOptions[selectedDayIndex]

        // 요일 선택 버튼 클릭 리스너
        binding.dayCardView.setOnClickListener {
            showDaySelectionDialog()
        }

        // 시작 시간 선택 버튼 - 바텀 시트 사용
        binding.time1CardView.setOnClickListener {
            showTimePickerBottomSheet(true)
        }

        // 종료 시간 선택 버튼 - 바텀 시트 사용
        binding.time2CardView.setOnClickListener {
            showTimePickerBottomSheet(false)
        }

        // 기본 시간 표시 업데이트
        updateTimeDisplay()

        // 저장하기 버튼 클릭 리스너
        binding.btnSave.setOnClickListener {
            if (isDeleteMode && selectedEntry != null) {
                // 삭제 모드일 때
                viewModel.removeTimetableEntryFromServer(selectedEntry!!)
                selectedEntry = null
                isDeleteMode = false
                updateSaveButtonState()
            } else {
                // 저장 모드일 때
                if (areAllFieldsFilled()) {
                    addTimetableEntryRealtime()
                } else {
                    // 입력 필드 검증 실패 시 아무것도 하지 않음
                }
            }
        }

        // 초기화 버튼 - 오른쪽 상단으로 이동
        binding.btnClear.setOnClickListener {
            showClearConfirmationDialogRealtime()
        }

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            // 변경사항 저장 없이 돌아가기
            parentFragmentManager.popBackStack()
        }

        // 텍스트 변경 리스너 추가
        binding.etTitle.addTextChangedListener {
            updateSaveButtonState()
        }

        binding.etLocation.addTextChangedListener {
            updateSaveButtonState()
        }

        // 초기 요일 선택은 생성자에서 이미 설정됨 (selectedDayIndex = 0)
    }

    // 입력 완성도 확인
    private fun areAllFieldsFilled(): Boolean {
        val title = binding.etTitle.text?.toString()?.trim() ?: ""
        val location = binding.etLocation.text?.toString()?.trim() ?: ""
        return title.isNotEmpty() && location.isNotEmpty()
    }

    // 저장하기 버튼 상태 업데이트 - 회의 제안 바텀시트 스타일로 변경
    private fun updateSaveButtonState() {
        val context = context ?: return
        
        if (isDeleteMode) {
            // 삭제 모드
            binding.btnText.text = "삭제하기"
            binding.btnBg.setColorFilter(ContextCompat.getColor(context, R.color.red_color))
            binding.btnText.setTextColor(ContextCompat.getColor(context, R.color.white))
            binding.btnSave.isEnabled = true
            binding.btnSave.isClickable = true
            binding.btnSave.alpha = 1.0f
        } else {
            // 저장 모드
            binding.btnText.text = "저장하기"
            val isValid = areAllFieldsFilled()
            
            if (isValid) {
                // 활성화 상태
                binding.btnBg.setColorFilter(ContextCompat.getColor(context, R.color.mainColor))
                binding.btnText.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.btnSave.isEnabled = true
                binding.btnSave.isClickable = true
                binding.btnSave.alpha = 1.0f
            } else {
                // 비활성화 상태
                binding.btnBg.setColorFilter(ContextCompat.getColor(context, R.color.btn_inactive))
                binding.btnText.setTextColor(ContextCompat.getColor(context, R.color.black))
                binding.btnSave.isEnabled = false
                binding.btnSave.isClickable = false
                binding.btnSave.alpha = 0.5f
            }
        }
    }

    // TimeSelectionViewModel 관찰
    private fun setupTimeSelectionObservers() {
        // 시작 시간 관찰
        timeSelectionViewModel.startTime.observe(viewLifecycleOwner, Observer { startTime ->
            startTime?.let { (hour, minute) ->
                selectedStartHour = hour
                selectedStartMinute = minute
                updateTimeDisplay()
            }
        })

        // 종료 시간 관찰
        timeSelectionViewModel.endTime.observe(viewLifecycleOwner, Observer { endTime ->
            endTime?.let { (hour, minute) ->
                selectedEndHour = hour
                selectedEndMinute = minute
                updateTimeDisplay()
            }
        })
    }

    private fun observeEvents() {
        // 시간표 이벤트 관찰
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.timetableEvent.collect { event ->
                when (event) {
                    is ProfileViewModel.TimetableEvent.Added -> {
                        // 실시간 일정 추가 성공
                        clearInputFields()
                        // 시간표 UI 즉시 갱신
                        updateTimetableFromServer()
                    }
                    is ProfileViewModel.TimetableEvent.Removed -> {
                        // 실시간 일정 삭제 성공
                        // 시간표 UI 즉시 갱신
                        updateTimetableFromServer()
                        // 삭제 모드 해제
                        isDeleteMode = false
                        selectedEntry = null
                        updateSaveButtonState()
                    }
                    is ProfileViewModel.TimetableEvent.Cleared -> {
                        // 실시간 시간표 초기화 성공
                        // 시간표 UI 즉시 갱신
                        updateTimetableFromServer()
                        // 삭제 모드 해제
                        isDeleteMode = false
                        selectedEntry = null
                        updateSaveButtonState()
                    }
                    is ProfileViewModel.TimetableEvent.Conflict -> {
                        // 일정 충돌 발생
                        showConflictDialogRealtime(event.existing, event.new)
                    }
                    is ProfileViewModel.TimetableEvent.Error -> {
                        // 에러 발생 - 메시지 표시 제거
                    }
                    is ProfileViewModel.TimetableEvent.SyncCompleted -> {
                        // 동기화 완료 (성능 최적화: 로그 제거)
                        if (!event.success) {
                            // 동기화 오류 메시지 제거
                        }
                    }
                    else -> { /* 다른 이벤트 처리 */ }
                }
            }
        }
        
        // 시간표 데이터 변경 관찰 (실시간 업데이트 반영)
        viewModel.timetable.observe(viewLifecycleOwner) { timetableList ->
            // 서버에서 받은 최신 데이터로 임시 리스트 동기화
            temporaryEntries.clear()
            temporaryEntries.addAll(timetableList)
            
            // 서버에서 받은 최신 데이터로 미리보기 업데이트
            updateTimetablePreviewFromServer(timetableList)
        }
    }

    // 실시간 일정 추가 (즉시 서버 저장)
    private fun addTimetableEntryRealtime() {
        val title = binding.etTitle.text?.toString()?.trim() ?: ""
        val location = binding.etLocation.text?.toString()?.trim() ?: ""

        if (title.isEmpty()) {
            return
        }

        // 선택한 요일 인덱스 (1: 월, 2: 화, ..., 7:일)
        val dayIndex = selectedDayIndex + 1

        // TimetableEntry 생성 (수정된 버전 - 분 정보 포함)
        val entryWithoutColor = TimetableEntry(
            title = title,
            dayOfWeek = dayIndex,
            startHour = selectedStartHour,
            startMinute = selectedStartMinute,
            endHour = selectedEndHour,
            endMinute = selectedEndMinute,
            colorHex = defaultColor, // 임시 기본값
            location = location
        )
        
        // 색상 관리자를 사용하여 색상 배정
        val entry = colorManager.assignColor(entryWithoutColor)

        // 즉시 서버에 추가 (실시간)
        viewModel.addTimetableEntryToServer(entry)
        
        // 성공 메시지는 observeEvents에서 처리됨
        clearInputFields()
    }

    // 바텀 시트 방식 시간 선택
    private fun showTimePickerBottomSheet(isStartTime: Boolean) {
        // 대상 시간 및 초기값 설정
        val targetTimeView = if (isStartTime) "time1" else "time2"
        val initialHour = if (isStartTime) selectedStartHour else selectedEndHour
        val initialMinute = if (isStartTime) selectedStartMinute else selectedEndMinute

        // 새로운 바텀 시트 생성 및 표시
        val timePickerBottomSheet = EditTimePickerBottomSheet.newInstance(
            targetTimeView,
            initialHour,
            initialMinute
        )

        timePickerBottomSheet.show(parentFragmentManager, "EditTimePickerBottomSheet")
    }

    private fun updateTimeDisplay() {
        // 시간 형식 포맷팅 (09:00 또는 09:30 형식)
        val startFormatted = String.format("%02d:%02d", selectedStartHour, selectedStartMinute)
        val endFormatted = String.format("%02d:%02d", selectedEndHour, selectedEndMinute)

        binding.btnStartTime.text = startFormatted
        binding.btnEndTime.text = endFormatted
    }

    // 시간표 미리보기 업데이트 - 30분 단위 정확한 표시 (임시 데이터 사용)
    private fun updateTimetablePreview() {
        updateTimetablePreviewWithEntries(temporaryEntries)
    }

    // 공통 시간표 미리보기 생성 메서드 (실제 구현)
    private fun updateTimetablePreviewWithEntries(entries: List<TimetableEntry>) {
        val timetableLayout = binding.timetablePreview
        timetableLayout.removeAllViews()

        // 동적 시간 범위 계산 - 24시까지 표시 가능하도록 수정
        var minHour = 8  // 8시부터 시작 (기본값 변경)
        var maxHour = 24  // 최대 24시까지 허용 (21시 이후 잘림 해결)

        // 일정이 있는 경우에만 범위 조정
        if (entries.isNotEmpty()) {
            // 시작 시간 최소값 (시간 + 분/60으로 소수점 시간)
            val startTimes = entries.map {
                it.startHour + (it.startMinute / 60.0)
            }
            // 종료 시간 최대값 (시간 + 분/60으로 소수점 시간)
            val endTimes = entries.map {
                it.endHour + (it.endMinute / 60.0)
            }

            if (startTimes.minOrNull()?.toInt() ?: minHour < minHour) {
                minHour = (startTimes.minOrNull() ?: minHour.toDouble()).toInt()
            }

            if (ceil(endTimes.maxOrNull() ?: maxHour.toDouble()).toInt() > maxHour) {
                maxHour = ceil(endTimes.maxOrNull() ?: maxHour.toDouble()).toInt()
            }
        }

        // 시간 범위가 넘어가면 제한 (0-24 범위 내로)
        minHour = minHour.coerceIn(0, 23)
        maxHour = maxHour.coerceIn(minHour + 1, 24)  // 최대 24시까지 표시 가능하도록 수정

        // 시간표 테이블 생성
        val tableLayout = TableLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // 각 열을 stretchable로 설정
            for (i in 1..7) {
                setColumnStretchable(i, true)
            }
            setBackgroundColor(Color.WHITE)
        }

        // 요일 배열
        val weekDays = arrayOf("", "월", "화", "수", "목", "금", "토", "일")

        // 요일 헤더 행 추가
        val headerRow = TableRow(requireContext())

        // 빈 셀 (왼쪽 상단)
        val emptyCell = TextView(requireContext()).apply {
            text = ""
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            layoutParams = TableRow.LayoutParams().apply {
                width = 40
                height = TableRow.LayoutParams.WRAP_CONTENT
            }
        }
        headerRow.addView(emptyCell)

        // 요일 헤더 셀들
        for (i in 1 until weekDays.size) {
            val dayCell = TextView(requireContext()).apply {
                text = weekDays[i]
                textSize = 12f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.WHITE)
                layoutParams = TableRow.LayoutParams().apply {
                    width = 0
                    height = TableRow.LayoutParams.WRAP_CONTENT
                    weight = 1f
                }
                setPadding(4, 10, 4, 10)
            }
            headerRow.addView(dayCell)
        }
        tableLayout.addView(headerRow)

        // 1시간 단위 셀 높이 설정 (30분 단위 정확한 위치는 내부적으로 계산)
        val cellHeight = resources.getDimensionPixelSize(R.dimen.timetable_cell_height)

        // 시간 행 추가 (1시간 단위로 UI 표시)
        for (hour in minHour until maxHour) {
            val row = TableRow(requireContext())

            // 시간 셀 - 24시 처리
            val timeCell = TextView(requireContext()).apply {
                text = if (hour == 24) "24" else hour.toString() // 24시를 명시적으로 표시
                textSize = 10f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.WHITE)
                layoutParams = TableRow.LayoutParams().apply {
                    width = 40
                    height = cellHeight
                }
            }
            row.addView(timeCell)

            // 요일별 셀
            for (day in 1 until weekDays.size) {
                // 해당 시간에 해당하는 일정 찾기
                val entriesInThisHour = entries.filter { e ->
                    // 일정 시작 및 종료 시간 (분 단위)
                    val startTime = e.startHour * 60 + e.startMinute
                    val endTime = e.endHour * 60 + e.endMinute
                    
                    // 현재 시간 범위 (분 단위)
                    val hourStart = hour * 60
                    val hourEnd = (hour + 1) * 60
                    
                    // 해당 요일에 일정이 현재 시간 범위와 겹치는지 확인
                    e.dayOfWeek == day && 
                            !(endTime <= hourStart || startTime >= hourEnd)
                }

                if (entriesInThisHour.isEmpty()) {
                    // 빈 셀 추가
                    val emptyCell = LinearLayout(requireContext()).apply {
                        layoutParams = TableRow.LayoutParams(0, cellHeight, 1f)
                        setBackgroundResource(R.drawable.timetable_cell_border)
                    }
                    row.addView(emptyCell)
                } else {
                    // 현재 시간대의 일정 찾기 (여러 개 일정이 겹치는 경우 첫 번째 사용)
                    val entry = entriesInThisHour[0]
                    
                    // 현재 시간대에서 일정의 시작과 끝 위치 계산
                    val startTimeInMinutes = entry.startHour * 60 + entry.startMinute
                    val endTimeInMinutes = entry.endHour * 60 + entry.endMinute
                    
                    // 현재 시간대의 시작과 끝
                    val hourStartMinutes = hour * 60
                    val hourEndMinutes = (hour + 1) * 60
                    
                    // 일정의 상대적 위치 확인
                    val isStartHour = startTimeInMinutes >= hourStartMinutes && startTimeInMinutes < hourEndMinutes
                    val isEndHour = endTimeInMinutes > hourStartMinutes && endTimeInMinutes <= hourEndMinutes
                    
                    // 30분 단위 정확한 표시를 위한 개선된 셀 생성
                    val cell = LinearLayout(requireContext()).apply {
                        // 현재 시간대 내에서의 상대적 시작 위치 (0~1 사이 비율)
                        val startRatio = if (startTimeInMinutes <= hourStartMinutes) 0f
                                         else (startTimeInMinutes - hourStartMinutes) / 60f
                        
                        // 현재 시간대 내에서의 상대적 끝 위치 (0~1 사이 비율)
                        val endRatio = if (endTimeInMinutes >= hourEndMinutes) 1f
                                       else (endTimeInMinutes - hourStartMinutes) / 60f
                        
                        // 시작 위치에 따른 상단 마진 계산
                        val topMargin = (cellHeight * startRatio).toInt()
                        
                        // 시간 비율에 따른 높이 계산
                        val heightRatio = endRatio - startRatio
                        val cellContentHeight = (cellHeight * heightRatio).toInt()
                        
                        // 레이아웃 파라미터 설정 (상대적 위치로)
                        layoutParams = TableRow.LayoutParams(0, cellContentHeight, 1f).apply {
                            this.topMargin = topMargin
                        }
                        
                        gravity = Gravity.CENTER
                        orientation = LinearLayout.VERTICAL  // 수직 방향으로 설정

                        // 선택된 항목인지 확인해서 강조 표시
                        val isSelected = selectedEntry == entry
                        val backgroundColor = if (isSelected) {
                            // 선택된 상태: 더 진한 색상
                            val originalColor = Color.parseColor(entry.colorHex)
                            Color.argb(255, 
                                (Color.red(originalColor) * 0.8).toInt(),
                                (Color.green(originalColor) * 0.8).toInt(), 
                                (Color.blue(originalColor) * 0.8).toInt())
                        } else {
                            Color.parseColor(entry.colorHex)
                        }
                        setBackgroundColor(backgroundColor)

                        // 텍스트 표시 - 시작 시간에만 제목 표시, 끝 시간에 장소 표시 (시간 제거)
                        if (isStartHour) {
                            // 제목만 표시 (시간 제거)
                            addView(TextView(requireContext()).apply {
                                text = entry.title
                                textSize = 11f
                                gravity = Gravity.CENTER
                                setTextColor(Color.WHITE)
                                ellipsize = android.text.TextUtils.TruncateAt.END
                                maxLines = 1
                                setPadding(4, 4, 4, 4)
                            })
                        } else if (isEndHour && !entry.location.isNullOrEmpty() && 
                                   (endTimeInMinutes - startTimeInMinutes) >= 60) {
                            // 종료 시간에는 장소만 표시 (괄호와 시간 제거)
                            addView(TextView(requireContext()).apply {
                                text = "장소:${entry.location}"
                                textSize = 9f
                                gravity = Gravity.CENTER
                                setTextColor(Color.WHITE)
                                ellipsize = android.text.TextUtils.TruncateAt.END
                                maxLines = 1
                                setPadding(2, 0, 2, 0)
                            })
                        }

                        // 선택 및 삭제 모드 전환
                        setOnClickListener {
                            if (selectedEntry == entry) {
                                // 이미 선택된 항목 클릭 시 선택 해제
                                selectedEntry = null
                                isDeleteMode = false
                            } else {
                                // 새 항목 선택
                                selectedEntry = entry
                                isDeleteMode = true

                            }
                            updateSaveButtonState()
                            // 시간표 재갱신해서 선택 상태 반영
                            updateTimetablePreview()
                        }
                        
                        // 기본 알파값 설정
                        alpha = if (isSelected) 1.0f else 0.85f
                    }

                    row.addView(cell)
                }
            }

            tableLayout.addView(row)
        }
        
        // 시간표 높이를 동적으로 조정 (안전한 버전)
        try {
            val cellHeight = resources.getDimensionPixelSize(R.dimen.timetable_cell_height)
            val minHeight = cellHeight * 8 // 최소 8시간 표시
            val dynamicHeight = (maxHour - minHour) * cellHeight + 100 // 헤더 공간 추가
            val finalHeight = maxOf(minHeight, dynamicHeight)
            
            if (finalHeight > 0) {
                tableLayout.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    finalHeight
                )
            }
        } catch (e: Exception) {
            // 동적 높이 조정 실패 시 기본 높이 사용
            tableLayout.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        timetableLayout.addView(tableLayout)
    }

    // 실시간 충돌 다이얼로그 (서버 기반)
    private fun showConflictDialogRealtime(existing: TimetableEntry, new: TimetableEntry) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("시간 충돌")
            .setMessage("이미 '${existing.title}'(이)가 있는 시간입니다. 덮어쓰시겠습니까?")
            .setPositiveButton("덮어쓰기") { _, _ ->
                // 기존 항목을 서버에서 삭제
                viewModel.removeTimetableEntryFromServer(existing)
                // 새 항목을 서버에 추가
                viewModel.addTimetableEntryToServer(new)
                clearInputFields()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    // 서버에서 최신 시간표 데이터를 가져와서 UI 업데이트
    private fun updateTimetableFromServer() {
        val preferencesUtil = PreferencesUtil(requireContext())
        val userId = preferencesUtil.getUserId()
        
        if (userId != null) {
            lifecycleScope.launch {
                viewModel.loadTimetableFromServer(userId)
            }
        }
    }

    // 서버 데이터 기반 시간표 미리보기 업데이트
    private fun updateTimetablePreviewFromServer(entries: List<TimetableEntry>) {
        updateTimetablePreviewWithEntries(entries)
    }

    // 실시간 초기화 확인 다이얼로그
    private fun showClearConfirmationDialogRealtime() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("시간표 초기화")
            .setMessage("시간표를 모두 초기화하시겠습니까? 서버에서 즉시 삭제됩니다.")
            .setPositiveButton("초기화") { _, _ ->
                // 실시간 서버에서 초기화
                viewModel.clearTimetableFromServer()
                
                // 로컬 색상 매핑 초기화
                colorManager.clearColorMappings()
                selectedEntry = null
                isDeleteMode = false
                updateSaveButtonState()
                
                // 성공 메시지는 observeEvents에서 처리됨
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    // 요일 선택 다이얼로그 표시
    private fun showDaySelectionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("요일 선택")
            .setSingleChoiceItems(dayOptions, selectedDayIndex) { dialog, which ->
                selectedDayIndex = which
                binding.btnDaySelect.text = dayOptions[selectedDayIndex]
                
                // TimeSelectionViewModel에 날짜 설정 (오늘 날짜에서 요일만 변경)
                val dayIndex = selectedDayIndex + 1
                val today = LocalDate.now()
                val selectedDate = today.with(java.time.DayOfWeek.of(dayIndex))
                timeSelectionViewModel.updateSelectedDate(selectedDate)
                
                updateSaveButtonState()
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun clearInputFields() {
        binding.etTitle.setText("")
        binding.etLocation.setText("")
        updateSaveButtonState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}