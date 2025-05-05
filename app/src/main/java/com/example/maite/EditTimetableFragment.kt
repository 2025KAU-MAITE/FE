package com.example.maite.ui.profile

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.maite.R
import com.example.maite.databinding.FragmentEditTimetableBinding
import com.example.maite.model.TimetableEntry
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.ceil
import com.example.maite.PreferencesUtil

class EditTimetableFragment : Fragment() {

    private var _binding: FragmentEditTimetableBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by activityViewModels()

    // 독립적인 EditTimeSelectionViewModel 사용
    private val timeSelectionViewModel: EditTimeSelectionViewModel by activityViewModels()

    // 요일 선택 옵션
    private val dayOptions = arrayOf("월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일")

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

        setupUI()
        setupTimeSelectionObservers()
        observeEvents()

        // 초기 시간표 미리보기 표시
        updateTimetablePreview()
    }

    private fun setupUI() {
        // 요일 선택 드롭다운 설정
        val dayAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            dayOptions
        )
        binding.spinnerDay.adapter = dayAdapter

        // 요일 선택 리스너
        binding.spinnerDay.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // 선택된 요일은 1부터 시작하지만, LocalDate의 DayOfWeek는 월요일이 1
                val dayIndex = position + 1

                // TimeSelectionViewModel에 날짜 설정 (오늘 날짜에서 요일만 변경)
                val today = LocalDate.now()
                val selectedDate = today.with(java.time.DayOfWeek.of(dayIndex))
                timeSelectionViewModel.updateSelectedDate(selectedDate)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // 아무것도 하지 않음 (기본 선택 유지)
            }
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

        // 취소 버튼 (선택한 항목 삭제)
        binding.btnCancelEntry.setOnClickListener {
            if (selectedEntry != null) {
                // 선택된 항목이 있으면 임시 목록에서 제거
                temporaryEntries.remove(selectedEntry)
                selectedEntry = null
                updateTimetablePreview()
                Toast.makeText(requireContext(), "선택한 일정이 취소되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "취소할 일정을 시간표에서 선택해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        // 저장 버튼
        binding.btnSave.setOnClickListener {
            // 임시 시간표를 실제 시간표로 적용
            viewModel.updateTimetable(temporaryEntries)

            // 서버에 저장
            val userId = PreferencesUtil(requireContext()).getUserId()
            if (userId != null) {
                viewModel.saveTimetableToServer(userId)
            }

            Toast.makeText(requireContext(), "시간표가 저장되었습니다.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
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

        // 초기 요일 선택 (월요일)
        binding.spinnerDay.setSelection(0)
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
        val dayIndex = binding.spinnerDay.selectedItemPosition + 1

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

    private fun updateTimetablePreview() {
        val timetableLayout = binding.timetablePreview
        timetableLayout.removeAllViews()

        // 시간표 생성을 위한 데이터
        val entries = temporaryEntries


        var minHour = 9
        var maxHour = 24

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

        // 시간 범위가 넘어가면 제한 (0-23 범위 내로)
        minHour = minHour.coerceIn(0, 23)
        maxHour = maxHour.coerceIn(minHour + 1, 23)

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

        // 시간대별 행 추가 (30분 단위로 변경)
        for (timeSlot in (minHour * 2)..(maxHour * 2)) {
            val hour = timeSlot / 2
            val minute = (timeSlot % 2) * 30
            val currentTimeInMinutes = hour * 60 + minute

            val row = TableRow(requireContext())

            // 시간 셀
            val timeCell = TextView(requireContext()).apply {
                text = String.format("%02d:%02d", hour, minute)
                textSize = 10f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#F5F5F5"))
                layoutParams = TableRow.LayoutParams().apply {
                    width = 40
                    height = 30 // 30분 단위이므로 높이 조정
                }
            }
            row.addView(timeCell)

            // 요일별 셀
            for (day in 1 until weekDays.size) {
                // 현재 시간대의 일정 찾기
                val entry = entries.find { e ->
                    val startTimeInMinutes = e.startHour * 60 + e.startMinute
                    val endTimeInMinutes = e.endHour * 60 + e.endMinute

                    e.dayOfWeek == day &&
                            currentTimeInMinutes >= startTimeInMinutes &&
                            currentTimeInMinutes < endTimeInMinutes
                }

                val cell = LinearLayout(requireContext()).apply {
                    layoutParams = TableRow.LayoutParams().apply {
                        width = 0
                        height = 30 // 30분 단위이므로 높이 조정
                        weight = 1f
                    }
                    gravity = Gravity.CENTER
                    orientation = LinearLayout.VERTICAL  // 수직 방향으로 설정

                    if (entry != null) {
                        // 일정 시작 시간 및 종료 시간 (분 단위)
                        val startTimeInMinutes = entry.startHour * 60 + entry.startMinute
                        val endTimeInMinutes = entry.endHour * 60 + entry.endMinute

                        // 시작 시간의 다음 셀 (30분 후)
                        val isTitleCell = (
                                currentTimeInMinutes == startTimeInMinutes + 30
                        )

                        // 종료 시간의 이전 셀 (30분 전)
                        val isLocationCell = (
                                currentTimeInMinutes == endTimeInMinutes - 30
                        )

                        // 최소 길이 확인 (적어도 1시간 이상이어야 제목/장소 표시)
                        val isLongEnough = (endTimeInMinutes - startTimeInMinutes) >= 60
                        // 일정이 있는 경우
                        setBackgroundColor(Color.parseColor(entry.colorHex))
                        alpha = 0.85f

                        // 일정 시작 시간인 경우에만 제목 표시
                        val isStartTime = (
                                currentTimeInMinutes == entry.startHour * 60 + entry.startMinute
                                )

                        if (isLongEnough && isTitleCell) {
                            addView(TextView(requireContext()).apply {
                                text = entry.title
                                textSize = 9f
                                gravity = Gravity.CENTER
                                setTextColor(Color.WHITE)
                                ellipsize = android.text.TextUtils.TruncateAt.END
                                maxLines = 1
                                setPadding(2, 2, 2, 2)
                            })
                        } else if (isLongEnough && isLocationCell && !entry.location.isNullOrEmpty()) {
                            addView(TextView(requireContext()).apply {
                                text = "장소:${entry.location}"
                                textSize = 7f
                                gravity = Gravity.CENTER
                                setTextColor(Color.WHITE)
                                ellipsize = android.text.TextUtils.TruncateAt.END
                                maxLines = 1
                                setPadding(2, 0, 2, 0)
                            })
                        } else if (!isLongEnough && currentTimeInMinutes == startTimeInMinutes) {
                            addView(TextView(requireContext()).apply {
                                text = entry.title
                                textSize = 9f
                                gravity = Gravity.CENTER
                                setTextColor(Color.WHITE)
                                ellipsize = android.text.TextUtils.TruncateAt.END
                                maxLines = 1
                                setPadding(2, 2, 2, 2)
                            })
                        }

                        // 선택 가능하도록 설정
                        setOnClickListener {
                            // 이전에 선택된 항목이 있으면 강조 해제
                            selectedEntry?.let { prevEntry ->
                                // 모든 셀을 찾아서 강조 해제 (findCell 사용 안 함)
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
                    } else {
                        // 빈 셀
                        setBackgroundResource(R.drawable.timetable_cell_border)
                    }
                }

                row.addView(cell)
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
                Toast.makeText(requireContext(), "시간표가 초기화되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun clearInputFields() {
        binding.etTitle.setText("")
        binding.etLocation.setText("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}