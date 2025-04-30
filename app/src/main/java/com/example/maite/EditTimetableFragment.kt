package com.example.maite.ui.profile

import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.maite.R
import com.example.maite.databinding.FragmentEditTimetableBinding
import com.example.maite.model.TimetableEntry
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.*

class EditTimetableFragment : Fragment() {

    private var _binding: FragmentEditTimetableBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by activityViewModels()

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

        // 시작 시간 선택 버튼
        binding.btnStartTime.setOnClickListener {
            showTimePickerDialog(true)
        }

        // 종료 시간 선택 버튼
        binding.btnEndTime.setOnClickListener {
            showTimePickerDialog(false)
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
            Toast.makeText(requireContext(), "시간표가 저장되었습니다.", Toast.LENGTH_SHORT).show()

            // 프로필 화면으로 돌아가기
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

        // 선택한 요일 인덱스 (1: 월, 2: 화, ...)
        val dayIndex = binding.spinnerDay.selectedItemPosition + 1

        // TimetableEntry 생성
        val entry = TimetableEntry(
            title = title,
            dayOfWeek = dayIndex,
            startHour = selectedStartHour,
            endHour = selectedEndHour,
            colorHex = defaultColor,
            location = location
        )

        // 임시 시간표에 충돌 검사 후 추가
        val conflictingEntry = temporaryEntries.find { existing ->
            existing.dayOfWeek == entry.dayOfWeek && (
                    // 새 일정이 기존 일정과 겹치는지 확인
                    (entry.startHour < existing.endHour && entry.endHour > existing.startHour) ||
                            // 기존 일정이 새 일정을 포함하는지 확인
                            (existing.startHour <= entry.startHour && existing.endHour >= entry.endHour) ||
                            // 새 일정이 기존 일정을 포함하는지 확인
                            (entry.startHour <= existing.startHour && entry.endHour >= existing.endHour)
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

    private fun showTimePickerDialog(isStartTime: Boolean) {
        val initialHour = if (isStartTime) selectedStartHour else selectedEndHour
        val initialMinute = if (isStartTime) selectedStartMinute else selectedEndMinute

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                // 30분 단위로 반올림 (0 또는 30분)
                val roundedMinute = if (minute < 15) 0 else if (minute < 45) 30 else 0
                val adjustedHour = if (minute >= 45) (hourOfDay + 1) % 24 else hourOfDay

                if (isStartTime) {
                    selectedStartHour = adjustedHour
                    selectedStartMinute = roundedMinute

                    // 시작 시간이 종료 시간보다 이후면 종료 시간도 조정
                    if (selectedStartHour > selectedEndHour ||
                        (selectedStartHour == selectedEndHour && selectedStartMinute >= selectedEndMinute)) {
                        selectedEndHour = (selectedStartHour + (if (selectedStartMinute == 30) 1 else 0)) % 24
                        selectedEndMinute = if (selectedStartMinute == 0) 30 else 0
                    }
                } else {
                    selectedEndHour = adjustedHour
                    selectedEndMinute = roundedMinute

                    // 종료 시간이 시작 시간보다 이전이면 시작 시간도 조정
                    if (selectedEndHour < selectedStartHour ||
                        (selectedEndHour == selectedStartHour && selectedEndMinute <= selectedStartMinute)) {
                        selectedStartHour = (selectedEndHour - (if (selectedEndMinute == 0) 1 else 0) + 24) % 24
                        selectedStartMinute = if (selectedEndMinute == 30) 0 else 30
                    }
                }

                updateTimeDisplay()
            },
            initialHour,
            initialMinute,
            true // 24시간 형식
        ).show()
    }

    private fun updateTimeDisplay() {
        // 시간 형식 포맷팅 (09:00 형식)
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

        // 고정 시간 범위 (09~24시)
        val minTime = 9
        val maxTime = 24

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

        // 시간대별 행 추가
        for (hour in minTime until maxTime) {
            val row = TableRow(requireContext())

            // 시간 셀
            val timeCell = TextView(requireContext()).apply {
                text = hour.toString()
                textSize = 10f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#F5F5F5"))
                layoutParams = TableRow.LayoutParams().apply {
                    width = 40
                    height = 40 // 미리보기이므로 높이를 좀 더 작게
                }
            }
            row.addView(timeCell)

            // 요일별 셀
            for (day in 1 until weekDays.size) {
                val entry = entries.find {
                    (hour in it.startHour until it.endHour) && it.dayOfWeek == day
                }

                val cell = LinearLayout(requireContext()).apply {
                    layoutParams = TableRow.LayoutParams().apply {
                        width = 0
                        height = 40 // 미리보기이므로 높이를 좀 더 작게
                        weight = 1f
                    }
                    gravity = Gravity.CENTER

                    if (entry != null) {
                        // 일정이 있는 경우
                        setBackgroundColor(Color.parseColor(entry.colorHex))
                        alpha = 0.85f

                        // 선택 가능하도록 설정
                        setOnClickListener {
                            // 이전에 선택된 항목이 있으면 강조 해제
                            selectedEntry?.let { prevEntry ->
                                val prevCell = findCell(tableLayout, prevEntry)
                                prevCell?.alpha = 0.85f
                            }

                            // 새 항목 선택 및 강조
                            selectedEntry = entry
                            alpha = 1.0f
                            Toast.makeText(context, "${entry.title} 선택됨", Toast.LENGTH_SHORT).show()
                        }

                        // 일정 제목 표시 (미리보기이므로 글자 크기 작게)
                        addView(TextView(requireContext()).apply {
                            text = entry.title
                            textSize = 9f
                            gravity = Gravity.CENTER
                            setTextColor(Color.WHITE)
                            ellipsize = android.text.TextUtils.TruncateAt.END
                            maxLines = 1
                            setPadding(2, 2, 2, 2)
                        })
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

    // 특정 항목에 해당하는 셀 찾기
    private fun findCell(tableLayout: TableLayout, entry: TimetableEntry): View? {
        for (hour in entry.startHour until entry.endHour) {
            val rowIndex = hour - 9 + 1 // 9시가 첫번째 행이므로 +1 (헤더 행 고려)
            if (rowIndex >= 0 && rowIndex < tableLayout.childCount) {
                val row = tableLayout.getChildAt(rowIndex) as? TableRow
                row?.let {
                    val cellIndex = entry.dayOfWeek // 요일 인덱스 사용
                    if (cellIndex < it.childCount) {
                        return it.getChildAt(cellIndex)
                    }
                }
            }
        }
        return null
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