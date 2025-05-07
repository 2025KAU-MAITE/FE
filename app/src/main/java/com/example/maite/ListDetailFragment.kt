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
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.maite.databinding.FragmentListDetailBinding
import com.example.maite.model.InviteUserRequest
import com.example.maite.model.MaiteListItem
import com.example.maite.viewmodel.InviteListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListDetailFragment : Fragment() {
    private var _binding: FragmentListDetailBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: TimeSelectionViewModel by activityViewModels()
    private lateinit var inviteViewModel: InviteListViewModel

    private lateinit var apiService: MaiteApiService
    private var maiteListItem: MaiteListItem? = null

    private val weekDays = arrayOf("", "월", "화", "수", "목", "금", "토", "일")
    private val timeSlots = Array(25) { String.format("%02d", it) } // 00 ~ 24
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

    private lateinit var availableDaysOfWeek: Set<Int>

    private var participantEmails: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListDetailBinding.inflate(inflater, container, false)
        availableDaysOfWeek = classes.map { it.dayOfWeek }.toSet()
        Log.d("ListDetailFragment", "사용 가능한 요일: $availableDaysOfWeek")
        sharedViewModel.setTimetableData(classes)
        Log.d("ListDetailFragment", "ViewModel에 시간표 데이터 설정 완료")

        apiService = MaiteRetrofitClient.getInstance(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inviteViewModel = ViewModelProvider(this)[InviteListViewModel::class.java]
        inviteViewModel.inviteList.observe(viewLifecycleOwner) { userList ->
            Log.d("ListDetailFragment", "사용자 목록 로드됨: ${userList.size}명")
        }

        maiteListItem = arguments?.getParcelable<MaiteListItem>(ARG_MAITE_LIST_ITEM)

        binding.title.text = maiteListItem?.title
        binding.intro.text = maiteListItem?.intro

        // 참가자 이메일 리스트 가져오기
        participantEmails = maiteListItem?.participantEmails ?: emptyList()
        Log.d("ListDetailFragment", "참가자 이메일 목록: $participantEmails")

        // 프로필 이미지 동적 추가
        updateParticipantProfiles()

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.addBtn.setOnClickListener {
            // 이메일을 기반으로 실제 사용자 ID 목록 가져오기
            val userList = inviteViewModel.inviteList.value ?: emptyList()

            // 이메일 목록과 일치하는 사용자의 ID 추출
            val selectedIds = userList
                .filter { item -> participantEmails.contains(item.email) }
                .map { it.id }

            Log.d("ListDetailFragment", "이메일로 매칭된 사용자 ID 목록: $selectedIds")

            // ListDetailFragment에서 호출됨을 나타내는 true 플래그 전달
            val bottomSheet = InviteBottomSheet.newInstance(selectedIds, true)
            bottomSheet.show(parentFragmentManager, bottomSheet.tag)
        }

        // InviteBottomSheet의 결과를 받기 위한 리스너 설정
        parentFragmentManager.setFragmentResultListener(
            InviteBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val selectedCount = bundle.getInt(InviteBottomSheet.KEY_SELECTED_COUNT, 0)
            val selectedIds = bundle.getSerializable(InviteBottomSheet.KEY_SELECTED_IDS) as? ArrayList<Long>
            val selectedEmails = bundle.getStringArrayList(InviteBottomSheet.KEY_SELECTED_EMAILS) ?: arrayListOf()

            Log.d("ListDetailFragment", "선택된 참가자 수: $selectedCount, 이메일: $selectedEmails")

            val roomId = maiteListItem?.roomId
            if (roomId != null) {
                // 새로 선택된 이메일 찾기 (기존에 없던 이메일)
                val newEmails = selectedEmails.filter { !participantEmails.contains(it) }

                if (newEmails.isNotEmpty()) {
                    // 새로 추가된 이메일들에 대해 초대 API 호출
                    inviteNewUsers(roomId, newEmails)
                }
            } else {
                Log.e("ListDetailFragment", "룸 ID가 null입니다.")
                Toast.makeText(requireContext(), "방 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }

            // 선택된 이메일로 참가자 목록 업데이트
            participantEmails = selectedEmails

            // UI 업데이트
            updateParticipantProfiles()
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

    private fun inviteNewUsers(roomId: Long, emails: List<String>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 각 이메일에 대해 API 호출
                for (email in emails) {
                    val request = InviteUserRequest(email)

                    // API 호출 수행
                    val response = withContext(Dispatchers.IO) {
                        apiService.inviteUserToRoom(roomId, request)
                    }

                    if (response.isSuccessful) {
                        Log.d("ListDetailFragment", "사용자 초대 성공: $email")
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("ListDetailFragment", "사용자 초대 실패 ($email): $errorBody")
                        // 초대 실패한 사용자가 있다면 토스트 메시지 표시
                        activity?.runOnUiThread {
                            Toast.makeText(requireContext(), "$email 초대 실패: $errorBody", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // 모든 초대가 완료되면 성공 메시지 표시
                if (emails.isNotEmpty()) {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "${emails.size}명의 사용자를 초대했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("ListDetailFragment", "사용자 초대 중 오류 발생", e)
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "초대 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    // 참가자 프로필 이미지 업데이트 함수
    private fun updateParticipantProfiles() {
        // 기존 프로필 이미지 초기화 (addBtn 제외)
        val participantsLayout = binding.invitedUsersLayout
        val childrenToRemove = mutableListOf<View>()

        for (i in 0 until participantsLayout.childCount) {
            val child = participantsLayout.getChildAt(i)
            if (child is ImageView && child.id != R.id.addBtn) {
                childrenToRemove.add(child)
            }
        }
        childrenToRemove.forEach { participantsLayout.removeView(it) }

        // 참가자 프로필 추가
        val imageSize = resources.getDimensionPixelSize(R.dimen.invited_profile_img_size)
        val desiredMarginDp = 8
        val imageMarginEnd = (desiredMarginDp * resources.displayMetrics.density).toInt()

        // addBtn의 인덱스 찾기
        val addBtn = participantsLayout.findViewById<ImageView>(R.id.addBtn)
        val addBtnIndex = if (addBtn != null) participantsLayout.indexOfChild(addBtn) else 0

        // 각 참가자 이메일에 대해 프로필 이미지 추가
        for (i in participantEmails.indices) {
            val email = participantEmails[i]

            val imageView = ImageView(requireContext())
            val layoutParams = LinearLayout.LayoutParams(imageSize, imageSize)
            layoutParams.marginEnd = imageMarginEnd
            imageView.layoutParams = layoutParams

            // 프로필 이미지 설정 (간단한 예시, 실제로는 참가자 프로필 URL을 API에서 받아와야 함)
            Glide.with(requireContext())
                .load(R.drawable.img_profile_default) // 기본 이미지 사용, 실제로는 프로필 URL 사용
                .apply(RequestOptions.circleCropTransform())
                .into(imageView)

            // 이메일 정보를 이미지 태그에 저장
            imageView.tag = email
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            // 클릭 리스너 추가 (선택 사항)
            imageView.setOnClickListener {
                Toast.makeText(requireContext(), "참가자: $email", Toast.LENGTH_SHORT).show()
            }

            // 레이아웃에 이미지 추가
            participantsLayout.addView(imageView, i)
        }

        // 스크롤 뷰를 오른쪽으로 스크롤
        binding.invitedUsersScrollView.post {
            binding.invitedUsersScrollView.fullScroll(View.FOCUS_RIGHT)
        }
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