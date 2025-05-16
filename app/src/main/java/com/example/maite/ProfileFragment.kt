package com.example.maite

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.maite.databinding.FragmentProfileBinding
import com.example.maite.model.TimetableEntry
import com.example.maite.model.UserInfo
import com.example.maite.ui.profile.EditTimetableFragment
import com.example.maite.ui.profile.ProfileEditBottomSheet
import com.example.maite.ui.profile.ProfileViewModel
import kotlin.math.ceil
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.maite.PreferencesUtil
import kotlinx.coroutines.launch


class ProfileFragment : Fragment(), ProfileEditBottomSheet.ProfileImageUpdateListener {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private val weekDays = arrayOf("", "월", "화", "수", "목", "금", "토", "일")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferencesUtil = PreferencesUtil(requireContext())
        val userId = preferencesUtil.getUserId()
        
        Log.d("ProfileFragment", "User ID from preferences: $userId")

        if (userId != null) {
            // 서버에서 시간표 불러오기
            Log.d("ProfileFragment", "Loading timetable for userId: $userId")
            lifecycleScope.launch {
                viewModel.loadTimetableFromServer(userId)
            }
            // 사용자 정보 불러오기
            viewModel.loadUserInfo(userId)
        } else {
            Log.e("ProfileFragment", "User ID not found")
        }

        // 사용자 정보 관찰
        viewModel.userInfo.observe(viewLifecycleOwner) { userInfo: UserInfo? ->
            userInfo?.let {
                binding.tvName.text = it.name
                binding.tvMateCount.text = "${it.mateCount}명의 Mate가 있습니다"

                // 프로필 이미지 로드 - null 안전 처리
                if (it.profileImageUrl != null && it.profileImageUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(it.profileImageUrl)
                        .circleCrop()
                        .placeholder(R.drawable.img_profile_default)
                        .error(R.drawable.img_profile_default)
                        .into(binding.ivProfile)
                }
            }
        }

        // 시간표 데이터 관찰
        viewModel.timetable.observe(viewLifecycleOwner) { timetableList ->
            Log.d("ProfileFragment", "Timetable data observed: ${timetableList.size} entries")
            createTimetable(timetableList)
        }

        // 시간표 수정 버튼 클릭 이벤트
        binding.btnEditTimetable.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, EditTimetableFragment())
                .addToBackStack(null)
                .commit()
        }

        // 프로필 이미지 클릭 이벤트 추가
        binding.ivProfile.setOnClickListener {
            showProfileEditBottomSheet()
        }

        // 상단 설정 버튼 클릭 이벤트 (추가 기능)
        binding.ivSettings.setOnClickListener {
            // TODO: 설정 화면으로 이동 또는 설정 메뉴 표시
        }
    }

    // 프로필 수정 바텀시트 표시
    private fun showProfileEditBottomSheet() {
        val bottomSheet = ProfileEditBottomSheet.newInstance()
        bottomSheet.show(childFragmentManager, ProfileEditBottomSheet.TAG)
    }

    // 프로필 이미지 업데이트 콜백
    override fun onProfileImageUpdated() {
        // 사용자 정보 새로고침 (프로필 이미지 업데이트)
        val userId = PreferencesUtil(requireContext()).getUserId()
        if (userId != null) {
            viewModel.loadUserInfo(userId)
        }
    }

    override fun onResume() {
        super.onResume()

        // 화면이 다시 보일 때마다 시간표 갱신
        val preferencesUtil = PreferencesUtil(requireContext())
        val userId = preferencesUtil.getUserId()
        if (userId != null) {
            Log.d("ProfileFragment", "onResume: Loading timetable for userId: $userId")
            lifecycleScope.launch {
                viewModel.loadTimetableFromServer(userId)
            }
        }
    }

    // 30분 단위로 시간표를 표시하도록 수정
    private fun createTimetable(entries: List<TimetableEntry>) {
        Log.d("ProfileFragment", "createTimetable called with ${entries.size} entries")
        
        val tableLayout = binding.timetableLayout
        tableLayout.removeAllViews()

        // 동적 시간 범위 계산
        var minHour = 9 // 기본 최소 시간 (9시)
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

            if ((startTimes.minOrNull() ?: minHour.toDouble()) < minHour) {
                minHour = (startTimes.minOrNull() ?: minHour.toDouble()).toInt()
            }

            if (ceil(endTimes.maxOrNull() ?: maxHour.toDouble()).toInt() > maxHour) {
                maxHour = ceil(endTimes.maxOrNull() ?: maxHour.toDouble()).toInt()
            }
        }

        // 시간 범위가 넘어가면 제한 (0-23 범위 내로)
        minHour = minHour.coerceIn(0, 23)
        maxHour = maxHour.coerceIn(minHour + 1, 23)

        // 시간 열 너비 계산
        val timeColWidth = calculateTextWidth("00")

        // 요일 헤더 행 추가
        val headerRow = TableRow(context)
        val timeHeaderCell = createTextView("", timeColWidth)
        timeHeaderCell.setBackgroundColor(Color.parseColor("#F5F5F5"))
        headerRow.addView(timeHeaderCell)

        for (i in 1 until weekDays.size) {
            val tv = createTextView(weekDays[i])
            tv.setBackgroundColor(Color.parseColor("#F5F5F5"))
            tv.textSize = 12f
            tv.setTextColor(Color.parseColor("#555555"))
            headerRow.addView(tv)
        }
        tableLayout.addView(headerRow)

        val cellHeight = resources.getDimensionPixelSize(R.dimen.timetable_cell_height) / 2 // 높이를 절반으로 조정

        for (timeSlot in (minHour * 2)..(maxHour * 2)) {
            val hour = timeSlot / 2
            val minute = (timeSlot % 2) * 30
            val currentTimeInMinutes = hour * 60 + minute

            val row = TableRow(context)

            // 시간 표시 열 - 정시(00분)에만 시간 표시
            val timeText = if (minute == 0) hour.toString() else ""
            val timeCell = createTextView(timeText, timeColWidth, cellHeight)
            timeCell.setBackgroundColor(Color.parseColor("#F5F5F5"))
            timeCell.textSize = 10f
            timeCell.setTextColor(Color.parseColor("#555555"))
            row.addView(timeCell)

            // 요일별 셀 추가
            for (day in 1 until weekDays.size) {
                // 해당 요일, 시간의 일정 찾기 (30분 단위 고려)
                val matched = entries.find { e ->
                    val startTimeInMinutes = e.startHour * 60 + e.startMinute
                    val endTimeInMinutes = e.endHour * 60 + e.endMinute

                    e.dayOfWeek == day &&
                            currentTimeInMinutes >= startTimeInMinutes &&
                            currentTimeInMinutes < endTimeInMinutes
                }

                // 셀 컨테이너 생성 - 홈 프라그먼트와 동일한 방식으로 변경
                val cell = LinearLayout(context).apply {
                    layoutParams = TableRow.LayoutParams(0, cellHeight, 1f)
                    gravity = Gravity.CENTER
                    orientation = LinearLayout.VERTICAL

                    if (matched != null) {
                        // 일정 시작 시간과 종료 시간 (분 단위)
                        val startTimeInMinutes = matched.startHour * 60 + matched.startMinute
                        val endTimeInMinutes = matched.endHour * 60 + matched.endMinute

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
                        setBackgroundColor(Color.parseColor(matched.colorHex))
                        alpha = 0.85f

                        // 일정 시작 시간인 경우에만 제목 표시
                        val isStartTime = (
                                currentTimeInMinutes == matched.startHour * 60 + matched.startMinute
                                )

                        if (isLongEnough && isTitleCell) {
                            addView(TextView(context).apply {
                                text = matched.title
                                textSize = 11f
                                gravity = Gravity.CENTER
                                setTextColor(Color.WHITE)
                                ellipsize = android.text.TextUtils.TruncateAt.END
                                maxLines = 1
                                setPadding(2, 2, 2, 2)
                            })
                        } else if (isLongEnough && isLocationCell && !matched.location.isNullOrEmpty()) {
                            addView(TextView(context).apply {
                                text = "장소:${matched.location}"
                                textSize = 7f
                                gravity = Gravity.CENTER
                                setTextColor(Color.WHITE)
                                ellipsize = android.text.TextUtils.TruncateAt.END
                                maxLines = 1
                                setPadding(2, 0, 2, 0)
                            })
                        } else if (!isLongEnough && currentTimeInMinutes == startTimeInMinutes) {
                            addView(TextView(context).apply {
                                text = matched.title
                                textSize = 11f
                                gravity = Gravity.CENTER
                                setTextColor(Color.WHITE)
                                ellipsize = android.text.TextUtils.TruncateAt.END
                                maxLines = 1
                                setPadding(2, 2, 2, 2)
                            })
                        }
                    } else {
                        // 빈 셀 - 홈 프라그먼트와 같이 테두리 설정
                        setBackgroundResource(R.drawable.timetable_cell_border)
                    }
                }

                row.addView(cell)
            }

            tableLayout.addView(row)
        }
    }

    // 셀 생성 함수 수정 - 요일 헤더와 시간 헤더에 사용됨
    private fun createTextView(text: String, width: Int = 0, height: Int = 0): TextView {
        val cellHeight = if (height > 0) height else resources.getDimensionPixelSize(R.dimen.timetable_cell_height)

        return TextView(context).apply {
            this.text = text
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(
                if (width > 0) width + 8 else 0,
                cellHeight
            ).apply {
                if (width <= 0) weight = 1f
            }
            // 헤더용 셀에 배경색만 적용 (테두리 없이)
            setBackgroundColor(Color.WHITE)
            maxLines = 1
            isSingleLine = true
        }
    }

    private fun calculateTextWidth(text: String): Int {
        val tv = TextView(context).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        tv.measure(0, 0)
        return tv.measuredWidth
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}