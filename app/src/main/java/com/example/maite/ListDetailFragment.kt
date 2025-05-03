package com.example.maite

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.LinearLayout
import android.view.Gravity
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import com.example.maite.databinding.FragmentListDetailBinding
import com.example.maite.model.MaiteListItem

class ListDetailFragment : Fragment() {
    private var _binding: FragmentListDetailBinding? = null
    private val binding get() = _binding!!

    // ViewModel 추가 (Activity 스코프)
    private val sharedViewModel: TimeSelectionViewModel by activityViewModels()

    // 요일 목록 (월~일)
    private val weekDays = arrayOf("", "월", "화", "수", "목", "금", "토", "일")

    // 시간 (00시부터 24시까지) - 인덱스 접근 위해 유지
    private val timeSlots = Array(25) { String.format("%02d", it) } // 00 ~ 24

    // 수업 데이터 예시 (시간, 요일, 강의명, 색상)
    private val classes = listOf(
        TimetableItem(10, 2, "머신러닝", Color.parseColor("#4C7EED")), // 화 10시
        TimetableItem(11, 2, "머신러닝", Color.parseColor("#4C7EED")), // 화 11시
        TimetableItem(12, 2, "머신러닝", Color.parseColor("#4C7EED")), // 화 12시

        TimetableItem(15, 2, "컴네", Color.parseColor("#4C7EED")), // 화 15시
        TimetableItem(16, 2, "컴네", Color.parseColor("#4C7EED")), // 화 16시

        TimetableItem(10, 3, "딥러닝", Color.parseColor("#4C7EED")), // 수 10시
        TimetableItem(11, 3, "딥러닝", Color.parseColor("#4C7EED")), // 수 11시
        TimetableItem(12, 3, "딥러닝", Color.parseColor("#4C7EED")), // 수 12시

        TimetableItem(15, 3, "산학", Color.parseColor("#4C7EED")), // 수 15시
        TimetableItem(16, 3, "산학", Color.parseColor("#4C7EED")), // 수 16시
        TimetableItem(17, 3, "산학", Color.parseColor("#4C7EED")), // 수 17시

        TimetableItem(13, 7, "알바", Color.parseColor("#4C7EED")), // 일 13시
        TimetableItem(14, 7, "알바", Color.parseColor("#4C7EED")), // 일 14시
        TimetableItem(15, 7, "알바", Color.parseColor("#4C7EED")), // 일 15시
        TimetableItem(16, 7, "알바", Color.parseColor("#4C7EED")), // 일 16시
        TimetableItem(17, 7, "알바", Color.parseColor("#4C7EED")), // 일 17시
    )

    // 사용 가능한 요일 Set
    private lateinit var availableDaysOfWeek: Set<Int>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListDetailBinding.inflate(inflater, container, false)
        availableDaysOfWeek = classes.map { it.dayOfWeek }.toSet()
        Log.d("ListDetailFragment", "사용 가능한 요일: $availableDaysOfWeek")
        sharedViewModel.setTimetableData(classes)
        Log.d("ListDetailFragment", "ViewModel에 시간표 데이터 설정 완료")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val maiteListItem = arguments?.getParcelable<MaiteListItem>(ARG_MAITE_LIST_ITEM)

        binding.title.text = maiteListItem?.title
        binding.intro.text = maiteListItem?.intro

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.addBtn.setOnClickListener {
            val bottomSheet = InviteBottomSheet()
            bottomSheet.show(parentFragmentManager, bottomSheet.tag)
        }

        binding.timetableLayout.setOnClickListener {
            val availableDaysList = ArrayList(availableDaysOfWeek)
            Log.d("ListDetailFragment", "SuggestBottomSheet 생성, 전달 요일: $availableDaysList")
            val suggestBottomSheet = SuggestBottomSheet.newInstance(availableDaysList)
            suggestBottomSheet.show(parentFragmentManager, suggestBottomSheet.tag)
        }

        binding.recentHamberger.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, MeetListFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.propHamberger.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, PropMeetFragment())
                .addToBackStack(null)
                .commit()
        }

        createTimetable()
    }

    // 시간표 생성 함수 (수정됨: 동적 시간 범위)
    private fun createTimetable() {
        val tableLayout = binding.root.findViewById<TableLayout>(R.id.timetableLayout)
        tableLayout.removeAllViews() // 기존 뷰 제거

        // --- 시간 범위 동적 계산 로직 ---
        val minTime: Int
        val maxTime: Int // 마지막으로 표시할 시간 인덱스 (포함)

        if (classes.isNotEmpty()) {
            minTime = classes.minOfOrNull { it.timeSlot }?.let { (it - 1).coerceAtLeast(0) } ?: 0
            maxTime = classes.maxOfOrNull { it.timeSlot }?.let { (it + 1).coerceAtMost(23) } ?: 23
        } else {
            minTime = 9 // 데이터 없을 시 기본 시작 시간
            maxTime = 17 // 데이터 없을 시 기본 종료 시간
            Log.w("ListDetailFragment", "시간표 데이터가 없어 기본 시간 범위($minTime ~ $maxTime) 사용")
        }
        // --- 시간 범위 계산 로직 끝 ---

        Log.d("ListDetailFragment", "시간표 생성 범위: $minTime 시 ~ $maxTime 시")

        val timeColumnWidth = calculateTextWidth("00") + 16 // 시간 셀 너비

        // 요일 헤더 추가
        val headerRow = TableRow(context)
        val headerParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
        headerRow.layoutParams = headerParams
        val timeHeaderCell = TextView(context).apply {
            text = ""
            layoutParams = TableRow.LayoutParams(timeColumnWidth, TableRow.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(Color.WHITE)
            setPadding(4, 8, 4, 8)
        }
        headerRow.addView(timeHeaderCell)
        for (i in 1 until weekDays.size) {
            val dayHeaderCell = TextView(context).apply {
                text = weekDays[i]
                gravity = Gravity.CENTER
                textSize = 12f
                setBackgroundColor(Color.WHITE)
                setPadding(4, 8, 4, 8)
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            }
            headerRow.addView(dayHeaderCell)
        }
        tableLayout.addView(headerRow)

        // 시간대별 행 추가 (minTime부터 maxTime까지)
        val cellHeight = resources.getDimensionPixelSize(R.dimen.timetable_cell_height)
        for (time in minTime..maxTime) {
            val row = TableRow(context)
            val rowParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, cellHeight)
            row.layoutParams = rowParams

            // 시간 셀 추가
            val timeCell = TextView(context).apply {
                text = timeSlots.getOrNull(time) ?: ""
                gravity = Gravity.CENTER
                textSize = 10f
                setBackgroundColor(Color.WHITE)
                setPadding(4, 4, 4, 4)
                layoutParams = TableRow.LayoutParams(timeColumnWidth, TableRow.LayoutParams.MATCH_PARENT)
            }
            row.addView(timeCell)

            // 요일별 셀 추가
            for (day in 1 until weekDays.size) {
                val classItem = classes.find { it.timeSlot == time && it.dayOfWeek == day }
                val containerView = LinearLayout(context).apply {
                    layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1f)
                    gravity = Gravity.CENTER
                    setBackgroundResource(R.drawable.timetable_cell_border)
                    orientation = LinearLayout.VERTICAL
                    minimumHeight = cellHeight
                }

                if (classItem != null) {
                    containerView.setBackgroundColor(classItem.color)
                    containerView.setOnLongClickListener {
                        Toast.makeText(context, classItem.className, Toast.LENGTH_SHORT).show()
                        true
                    }
                }
                row.addView(containerView)
            }
            tableLayout.addView(row)
        }
    }

    // 텍스트 너비 계산 함수
    private fun calculateTextWidth(text: String): Int {
        // context가 null일 수 있으므로 안전 호출 또는 requireContext() 사용
        val currentContext = context ?: return 0
        val textView = TextView(currentContext).apply {
            this.text = text
            this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }
        return textView.measuredWidth
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 시간표 아이템 데이터 클래스
    data class TimetableItem(
        val timeSlot: Int,   // 시간대 인덱스 (0~23)
        val dayOfWeek: Int,  // 요일 인덱스 (1: 월, ..., 7: 일)
        val className: String,
        val color: Int
    )

    // Companion object
    companion object {
        private const val ARG_MAITE_LIST_ITEM = "maite_list_item"
        fun newInstance(maiteListItem: MaiteListItem): ListDetailFragment {
            val fragment = ListDetailFragment()
            val args = Bundle().apply {
                putParcelable(ARG_MAITE_LIST_ITEM, maiteListItem)
            }
            fragment.arguments = args
            return fragment
        }
    }
}