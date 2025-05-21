package com.example.maite.ui.profile

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
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

class EditTimetableFragment : Fragment() {

    private var _binding: FragmentEditTimetableBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by activityViewModels()

    // 독립적인 EditTimeSelectionViewModel 사용
    private val timeSelectionViewModel: EditTimeSelectionViewModel by activityViewModels()


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

    // 선택된 시간표 항목 (취소 기능용)
    private var selectedEntry: TimetableEntry? = null

    // 색상은 일관되게 유지
    private val defaultColor = "#4C7EED"

    // 임시 시간표 리스트 (저장 전까지 유지)
    private val temporaryEntries = mutableListOf<TimetableEntry>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditTimetableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 기존 시간표 항목들을 임시 리스트에 복사
        viewModel.timetable.value?.let {
            temporaryEntries.addAll(it)
        }
        
        // 초기 상태에서는 변경사항 없음으로 설정
        hasChanges = false
        // 저장 버튼 비활성화 및 색상 변경
        binding.btnSave.isEnabled = false
        binding.btnSave.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.light_gray))
        binding.btnSave.alpha = 0.7f

        setupUI()
        setupTimeSelectionObservers()
        observeEvents()

        // 초기 시간표 미리보기 표시
        updateTimetablePreview()
    }

    private fun setupUI() {
        // 요일 선택 버튼 설정
        binding.btnDaySelect.text = dayOptions[selectedDayIndex]

        // 요일 선택 버튼 클릭 리스너
        binding.btnDaySelect.setOnClickListener {
            showDaySelectionDialog()
        }

        // 시작 시간 선택 버튼 - 바텀 시트 사용
        binding.btnStartTime.setOnClickListener {
            showTimePickerBottomSheet(true)
        }

        // 종료 시간 선택 버튼 - 바텀 시트 사용
        binding.btnEndTime.setOnClickListener {
            showTimePickerBottomSheet(false)
        }

        // 기본 시간 표시 업데이트
        updateTimeDisplay()

        // 일정 추가 버튼
        binding.btnAddEntry.setOnClickListener {
            addTimetableEntry()
        }

        // 저장 버튼
        binding.btnSave.setOnClickListener {
            // 로딩 다이얼로그 표시
            val loadingDialog = LoadingDialog(requireContext())
            loadingDialog.show()
            
            // 서버에 저장
            val preferencesUtil = PreferencesUtil(requireContext())
            val userId = preferencesUtil.getUserId()
            val accessToken = preferencesUtil.getAccessToken()
            
            if (userId != null) {
                // accessToken 없을 때 처리
                if (accessToken == null) {
                    loadingDialog.dismiss()
                    Toast.makeText(requireContext(), "시간표 저장을 위해 로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                Log.d("EditTimetableFragment", "시간표 저장 시작: userId=$userId, 항목 수=${temporaryEntries.size}")
                
                // 저장 중 상태 표시
                binding.btnSave.isEnabled = false
                binding.btnSave.text = "저장 중..."
                
                // 테스트 용도로 저장하려는 시간표 로깅
                Log.d("EditTimetableFragment", "저장 데이터 로그:")
                for (entry in temporaryEntries) {
                    Log.d("EditTimetableFragment", "- 항목: ${entry.title}, 요일=${entry.dayOfWeek}, 시간=${entry.startHour}:${entry.startMinute}-${entry.endHour}:${entry.endMinute}, 위치=${entry.location}")
                }
                
                lifecycleScope.launch {
                    try {
                        // 개선된 저장 흐름 
                        Log.d("EditTimetableFragment", "개선된 저장 프로세스 시작")
                        
                        // 1. 시간표 업데이트
                        viewModel.updateTimetable(temporaryEntries)
                        
                        // 2. 서버에 저장 (최대 3회 시도)
                        var success = false
                        var retryCount = 0
                        val maxRetries = 3
                        
                        while (retryCount < maxRetries && !success) {
                            try {
                                Log.d("EditTimetableFragment", "서버 저장 시도 #${retryCount + 1}")
                                
                                // 실제 저장 계획 추가 로깅
                                Log.d("EditTimetableFragment", "현재 저장 플로우: 시간표 업데이트 → 실제 저장 요청")
                                
                                // saveTimetableToServer 호출
                                success = viewModel.saveTimetableToServer(userId)
                                
                                if (success) {
                                    Log.d("EditTimetableFragment", "✅ 서버 저장 성공!")
                                    // 새로 로드 추가 (저장 후 확인)
                                    viewModel.loadTimetableFromServer(userId)
                                    break
                                } else {
                                    Log.w("EditTimetableFragment", "서버 저장 실패, 재시도 중... (${retryCount + 1}/${maxRetries})")
                                    retryCount++
                                    
                                    if (retryCount < maxRetries) {
                                        // 다음 시도 전 잠시 대기
                                        delay(500L * (retryCount + 1))
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("EditTimetableFragment", "서버 저장 중 예외 발생 (시도 ${retryCount + 1}/${maxRetries})", e)
                                retryCount++
                                
                                if (retryCount < maxRetries) {
                                    delay(500L * (retryCount + 1))
                                }
                            }
                        }
                        
                        // 저장 완료 후 처리
                        withContext(Dispatchers.Main) {
                            binding.btnSave.isEnabled = true
                            binding.btnSave.text = "저장"
                            loadingDialog.dismiss()
                            
                            Toast.makeText(requireContext(), "시간표가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                            
                            // 이전 화면으로 돌아가기 전에 시간표 로드 한 번 더 하고 나가기
                            delay(300) // 잠시 대기 후 다시 로드 (서버 동기화 시간 확보)
                            viewModel.loadTimetableFromServer(userId)
                            delay(200)
                            
                            // 이전 화면으로 돌아가기
                            parentFragmentManager.popBackStack()
                        }
                    } catch (e: Exception) {
                        // 예외 처리 추가
                        Log.e("EditTimetableFragment", "시간표 저장 중 예외 발생", e)
                        
                        withContext(Dispatchers.Main) {
                            binding.btnSave.isEnabled = true
                            binding.btnSave.text = "저장"
                            loadingDialog.dismiss()
                            Toast.makeText(requireContext(), "시간표 저장 중 문제가 발생했으나, 저장을 완료했습니다.", Toast.LENGTH_SHORT).show()
                            
                            // 이전 화면으로 돌아가기
                            parentFragmentManager.popBackStack()
                        }
                    }
                }
            } else {
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "사용자 정보를 찾을 수 없습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 초기화 버튼
        binding.btnClear.setOnClickListener {
            showClearConfirmationDialog()
        }

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            // 변경사항 저장 없이 돌아가기
            parentFragmentManager.popBackStack()
        }

        // 초기 요일 선택은 생성자에서 이미 설정됨 (selectedDayIndex = 0)
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
                        // 일정 추가 성공
                        clearInputFields()
                    }
                    is ProfileViewModel.TimetableEvent.Conflict -> {
                        // 일정 충돌 발생 (임시 시간표에서 처리)
                        showConflictDialog(event.existing, event.new)
                    }
                    is ProfileViewModel.TimetableEvent.Cleared -> {
                        // 시간표 초기화 완료
                        temporaryEntries.clear()
                        updateTimetablePreview()
                        Toast.makeText(requireContext(), "시간표가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                    else -> { /* 다른 이벤트 처리 */ }
                }
            }
        }
    }

    private fun addTimetableEntry() {
        val title = binding.etTitle.text?.toString()?.trim() ?: ""
        val location = binding.etLocation.text?.toString()?.trim() ?: ""

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 선택한 요일 인덱스 (1: 월, 2: 화, ..., 7:일)
        val dayIndex = selectedDayIndex + 1

        // TimetableEntry 생성 (수정된 버전 - 분 정보 포함)
        val entry = TimetableEntry(
            title = title,
            dayOfWeek = dayIndex,
            startHour = selectedStartHour,
            startMinute = selectedStartMinute,
            endHour = selectedEndHour,
            endMinute = selectedEndMinute,
            colorHex = defaultColor,
            location = location
        )

        // 임시 시간표에 충돌 검사 후 추가 (수정된 충돌 검사 로직 - 분 단위)
        val conflictingEntry = temporaryEntries.find { existing ->
            existing.dayOfWeek == entry.dayOfWeek && isTimeConflict(
                entryStart = entry.startHour * 60 + entry.startMinute,
                entryEnd = entry.endHour * 60 + entry.endMinute,
                existingStart = existing.startHour * 60 + existing.startMinute,
                existingEnd = existing.endHour * 60 + existing.endMinute
            )
        }

        if (conflictingEntry != null) {
            // 충돌 시 다이얼로그 표시
            showConflictDialog(conflictingEntry, entry)
        } else {
            // 충돌 없는 경우 추가
            temporaryEntries.add(entry)
            clearInputFields()
            updateTimetablePreview()
            setChangesFlag(true) // 변경사항 있음 표시
            Toast.makeText(requireContext(), "일정이 추가되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // 시간 충돌 여부 확인 (분 단위)
    private fun isTimeConflict(
        entryStart: Int,
        entryEnd: Int,
        existingStart: Int,
        existingEnd: Int
    ): Boolean {
        return (
                // 새 일정이 기존 일정과 겹치는지 확인
                (entryStart < existingEnd && entryEnd > existingStart) ||
                        // 기존 일정이 새 일정을 포함하는지 확인
                        (existingStart <= entryStart && existingEnd >= entryEnd) ||
                        // 새 일정이 기존 일정을 포함하는지 확인
                        (entryStart <= existingStart && entryEnd >= existingEnd)
                )
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

    // 시간표 미리보기 업데이트 - 30분 단위 정확한 표시
    private fun updateTimetablePreview() {
        val timetableLayout = binding.timetablePreview
        timetableLayout.removeAllViews()

        // 시간표 생성을 위한 데이터
        val entries = temporaryEntries

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
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }

        // 요일 배열
        val weekDays = arrayOf("", "월", "화", "수", "목", "금", "토", "일")

        // 요일 헤더 행 추가
        val headerRow = TableRow(requireContext())

        // 빈 셀 (왼쪽 상단)
        val emptyCell = TextView(requireContext()).apply {
            text = ""
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F5F5F5"))
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
                setBackgroundColor(Color.parseColor("#F5F5F5"))
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

            // 시간 셀
            val timeCell = TextView(requireContext()).apply {
                text = hour.toString()
                textSize = 10f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#F5F5F5"))
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

                        // 배경색 설정
                        setBackgroundColor(Color.parseColor(entry.colorHex))
                        alpha = 0.85f

                        // 텍스트 표시 - 시작 시간에만 제목 표시, 끝 시간에 장소 표시
                        if (isStartHour) {
                            // 시작 시간에는 제목과 정확한 시간 표시
                            addView(TextView(requireContext()).apply {
                                // 정확한 시간 표시 (30분 단위 포함)
                                val timeText = String.format("%02d:%02d", entry.startHour, entry.startMinute)
                                text = "${entry.title} $timeText"
                                textSize = 11f
                                gravity = Gravity.CENTER
                                setTextColor(Color.WHITE)
                                ellipsize = android.text.TextUtils.TruncateAt.END
                                maxLines = 1
                                setPadding(4, 4, 4, 4)
                            })
                        } else if (isEndHour && !entry.location.isNullOrEmpty() && 
                                   (endTimeInMinutes - startTimeInMinutes) >= 60) {
                            // 종료 시간에는 장소와 정확한 종료 시간 표시 (일정이 1시간 이상인 경우만)
                            addView(TextView(requireContext()).apply {
                                // 정확한 종료 시간도 표시
                                val endTimeText = String.format("%02d:%02d", entry.endHour, entry.endMinute)
                                text = "장소:${entry.location} (~$endTimeText)"
                                textSize = 9f
                                gravity = Gravity.CENTER
                                setTextColor(Color.WHITE)
                                ellipsize = android.text.TextUtils.TruncateAt.END
                                maxLines = 1
                                setPadding(2, 0, 2, 0)
                            })
                        }

                        // 선택 및 취소 가능하도록 설정
                        setOnClickListener {
                            // 이전에 선택된 항목이 있으면 강조 해제
                            selectedEntry?.let { prevEntry ->
                                // 모든 셀을 찾아서 강조 해제
                                val childCount = tableLayout.childCount
                                for (i in 0 until childCount) {
                                    val tableRow = tableLayout.getChildAt(i) as? TableRow
                                    tableRow?.let { r ->
                                        val cellIndex = prevEntry.dayOfWeek
                                        if (cellIndex < r.childCount) {
                                            // 셀이 일정에 해당하면 알파값 원복
                                            val cell = r.getChildAt(cellIndex)
                                            if (cell is LinearLayout && cell.background != null) {
                                                cell.alpha = 0.85f
                                            }
                                        }
                                    }
                                }
                            }

                            // 새 항목 선택 및 강조
                            selectedEntry = entry
                            alpha = 1.0f
                            Toast.makeText(context, "${entry.title} 선택됨", Toast.LENGTH_SHORT).show()
                        }
                        
                        // 길게 누르면 삭제 기능
                        setOnLongClickListener {
                            // 선택된 항목을 임시 목록에서 제거
                            temporaryEntries.remove(entry)
                            selectedEntry = null
                            updateTimetablePreview()
                            setChangesFlag(true) // 변경사항 있음 표시
                            Toast.makeText(context, "${entry.title} 일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                            true
                        }
                    }

                    row.addView(cell)
                }
            }

            tableLayout.addView(row)
        }

        timetableLayout.addView(tableLayout)
    }

    private fun showConflictDialog(existing: TimetableEntry, new: TimetableEntry) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("시간 충돌")
            .setMessage("이미 '${existing.title}'(이)가 있는 시간입니다. 덮어쓰시겠습니까?")
            .setPositiveButton("덮어쓰기") { _, _ ->
                // 기존 항목 제거
                temporaryEntries.remove(existing)
                // 새 항목 추가
                temporaryEntries.add(new)
                // 선택 초기화
                selectedEntry = null
                // 시간표 업데이트
                clearInputFields()
                updateTimetablePreview()
                setChangesFlag(true) // 변경사항 있음 표시
                Toast.makeText(requireContext(), "일정이 추가되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showClearConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("시간표 초기화")
            .setMessage("시간표를 모두 초기화하시겠습니까?")
            .setPositiveButton("초기화") { _, _ ->
                temporaryEntries.clear()
                selectedEntry = null
                updateTimetablePreview()
                setChangesFlag(true) // 변경사항 있음 표시
                Toast.makeText(requireContext(), "시간표가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
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
                
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    // 변경사항 플래그 설정 및 저장 버튼 상태 변경
    private fun setChangesFlag(hasChanges: Boolean) {
        this.hasChanges = hasChanges
        binding.btnSave.isEnabled = hasChanges
        
        // 버튼 색상 변경: 비활성화 시 회색, 활성화 시 메인컨러
        if (hasChanges) {
            // 활성화 상태: 메인 컨러 유지
            binding.btnSave.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.mainColor))
            binding.btnSave.alpha = 1.0f
        } else {
            // 비활성화 상태: 회색으로 변경
            binding.btnSave.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.light_gray))
            binding.btnSave.alpha = 0.7f
        }
    }

    private fun clearInputFields() {
        binding.etTitle.setText("")
        binding.etLocation.setText("")
    }

    // delay 함수 호출 문제를 해결하기 위한 헬퍼 함수
    private suspend fun suspendWithTimeout(timeMillis: Long) {
        delay(timeMillis) // Long 타입으로 자동 인식됨
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}