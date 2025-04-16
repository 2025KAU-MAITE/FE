package com.example.maite

import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
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
import com.example.maite.databinding.FragmentListDetailBinding
import com.example.maite.model.MaiteListItem

class ListDetailFragment : Fragment() {
    private var _binding: FragmentListDetailBinding? = null
    private val binding get() = _binding!!

    // 요일 목록 (월~일)
    private val weekDays = arrayOf("", "월", "화", "수", "목", "금", "토", "일")

    // 시간 (00시부터 24시까지) - 간단한 형식으로 변경
    private val timeSlots = arrayOf(
        "00", "01", "02", "03", "04", "05",
        "06", "07", "08", "09", "10", "11",
        "12", "13", "14", "15", "16", "17",
        "18", "19", "20", "21", "22", "23", "24"
    )

    // 수업 데이터 예시 (시간, 요일, 강의명, 색상)
    private val classes = listOf(
        TimetableItem(18, 1, "회의", Color.parseColor("#A5BEF5")),
        TimetableItem(19, 1, "회의", Color.parseColor("#A5BEF5")),

        TimetableItem(10, 2, "머신러닝", Color.parseColor("#4C7EED")),
        TimetableItem(11, 2, "머신러닝", Color.parseColor("#4C7EED")),
        TimetableItem(12, 2, "머신러닝", Color.parseColor("#4C7EED")),

        TimetableItem(15, 2, "컴네", Color.parseColor("#4C7EED")),
        TimetableItem(16, 2, "컴네", Color.parseColor("#4C7EED")),

        TimetableItem(10, 3, "딥러닝", Color.parseColor("#4C7EED")),
        TimetableItem(11, 3, "딥러닝", Color.parseColor("#4C7EED")),
        TimetableItem(12, 3, "딥러닝", Color.parseColor("#4C7EED")),

        TimetableItem(15, 3, "산학", Color.parseColor("#4C7EED")),
        TimetableItem(16, 3, "산학", Color.parseColor("#4C7EED")),
        TimetableItem(17, 3, "산학", Color.parseColor("#4C7EED")),

        TimetableItem(13, 7, "알바", Color.parseColor("#4C7EED")),
        TimetableItem(14, 7, "알바", Color.parseColor("#4C7EED")),
        TimetableItem(15, 7, "알바", Color.parseColor("#4C7EED")),
        TimetableItem(16, 7, "알바", Color.parseColor("#4C7EED")),
        TimetableItem(17, 7, "알바", Color.parseColor("#4C7EED")),

    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val maiteListItem = arguments?.getParcelable<MaiteListItem>(ARG_MAITE_LIST_ITEM)

        // 받아온 데이터를 UI에 설정
        binding.title.text = maiteListItem?.title
        binding.intro.text = maiteListItem?.intro

        // 뒤로 가기 버튼 클릭 리스너 설정
        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.addBtn.setOnClickListener {
            val bottomSheet = InviteBottomSheet()
            bottomSheet.show(parentFragmentManager, bottomSheet.tag)
        }

        // 시간표 생성
        createTimetable()
    }

    private fun createTimetable() {
        val tableLayout = binding.root.findViewById<TableLayout>(R.id.timetableLayout)

        // 수업 데이터에서 최소 시간과 최대 시간을 찾기
        val minTime = (classes.minOfOrNull { it.timeSlot } ?: 0) - 1
        val maxTime = (classes.maxOfOrNull { it.timeSlot } ?: 24) + 1

        // 시간 셀 너비 계산 (텍스트 크기에 맞게 최소화)
        val timeColumnWidth = calculateTextWidth("09")

        // 요일 헤더 추가
        val headerRow = TableRow(context)

        // 시간 헤더 (빈 칸)
        val timeHeaderCell = createTextView(weekDays[0], timeColumnWidth)
        timeHeaderCell.setBackgroundColor(Color.WHITE)
        timeHeaderCell.textSize = 12f
        timeHeaderCell.setPadding(2, 6, 2, 6)
        headerRow.addView(timeHeaderCell)

        // 요일 헤더들
        for (i in 1 until weekDays.size) {
            val textView = createTextView(weekDays[i])
            textView.setBackgroundColor(Color.WHITE)
            textView.textSize = 12f
            textView.setPadding(2, 6, 2, 6)
            val params = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            textView.layoutParams = params
            headerRow.addView(textView)
        }
        tableLayout.addView(headerRow)

        // 시간대별 행 추가 (얇은 바 형태로)
        val cellHeight = resources.getDimensionPixelSize(R.dimen.timetable_cell_height)

        for (i in minTime..maxTime) {
            val row = TableRow(context)

            // 시간 셀 추가 (숫자만 표시, 최소 너비로)
            val timeCell = createTextView(timeSlots.getOrNull(i) ?: "", timeColumnWidth)
            timeCell.setBackgroundColor(Color.WHITE)
            timeCell.textSize = 10f
            timeCell.setPadding(1, 2, 1, 2) // 패딩 최소화
            row.addView(timeCell)

            // 요일별 셀 추가 (월화수목금토일)
            for (j in 1 until weekDays.size) {
                // 해당 시간, 요일에 수업이 있는지 확인
                val classItem = classes.find { it.timeSlot == i && it.dayOfWeek == j }

                // 컨테이너 셀 생성
                val containerView = LinearLayout(context).apply {
                    layoutParams = TableRow.LayoutParams(0, cellHeight, 1f)
                    gravity = Gravity.TOP
                    setPadding(0, 0, 0, 0)
                    setBackgroundResource(R.drawable.timetable_cell_border)
                }

                if (classItem != null) {
                    // 일정이 있는 경우 세로 바 표시
                    val barView = View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT // ← 바 높이를 셀 전체에 맞춤
                        )
                        setBackgroundColor(classItem.color)
                    }
                    containerView.addView(barView)

                    // 롱클릭 시 수업명 표시
                    containerView.setOnLongClickListener {
                        // 토스트 메시지로 수업명 표시
                        android.widget.Toast.makeText(context, classItem.className, android.widget.Toast.LENGTH_SHORT).show()
                        true
                    }
                }

                row.addView(containerView)
            }

            tableLayout.addView(row)
        }
    }

    private fun createTextView(text: String, width: Int): TextView {
        return TextView(context).apply {
            this.text = text
            this.width = width + 8 // 여기서 기존 너비에 약간의 여유 (8 픽셀)를 추가합니다.
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(
                width + 8, // 여기서 기존 너비에 약간의 여유 (8 픽셀)를 추가합니다.
                resources.getDimensionPixelSize(R.dimen.timetable_cell_height)
            )
            setBackgroundResource(R.drawable.timetable_cell_border)
            maxLines = 1
            isSingleLine = true
        }
    }


    private fun createTextView(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            height = resources.getDimensionPixelSize(R.dimen.timetable_cell_height)
            setBackgroundResource(R.drawable.timetable_cell_border)
            maxLines = 1
            isSingleLine = true
        }
    }

    private fun calculateTextWidth(text: String): Int {
        val textView = TextView(context).apply {
            this.text = text
            this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f) // 시간 셀의 텍스트 크기에 맞춤
        }
        textView.measure(0, 0)
        return textView.measuredWidth
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

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

    // 시간표 아이템 데이터 클래스
    data class TimetableItem(
        val timeSlot: Int,   // 시간대 인덱스 (0~24)
        val dayOfWeek: Int,  // 요일 인덱스 (1: 월, 2: 화, 3: 수, 4: 목, 5: 금, 6: 토, 7: 일)
        val className: String, // 수업명
        val color: Int        // 배경색
    )
}