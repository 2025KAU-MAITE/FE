package com.example.maite

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.maite.databinding.FragmentHomeBinding
import com.example.maite.data.model.MeetingItem
import com.example.maite.data.model.MeetingProposal
import com.example.maite.model.TimetableEntry
import com.example.maite.ui.home.HomeViewModel
import kotlin.math.ceil

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ViewModel을 Activity 범위로 공유하여 상태 유지
    private val viewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 시간표 관찰 및 표시
        viewModel.timetableEntries.observe(viewLifecycleOwner) { entries ->
            renderTimetable(entries)
        }

        // 항상 라벨 표시
        binding.tvMeetingLabel.visibility = View.VISIBLE
        binding.tvProposalLabel.visibility = View.VISIBLE

        // 가장 가까운 회의 표시
        viewModel.nearestMeeting.observe(viewLifecycleOwner) { meeting: MeetingItem? ->
            if (meeting != null) {
                binding.cardMeeting.visibility = View.VISIBLE
                binding.tvMeetingTitle.text = meeting.title
                binding.tvMeetingDate.text = "날짜: ${meeting.date}"
                binding.tvMeetingTime.text = "시간: ${meeting.startTime} ~ ${meeting.endTime}"
                binding.tvMeetingLocation.text = "장소: ${meeting.location}"
                binding.tvNoMeetings.visibility = View.GONE
            } else {
                binding.cardMeeting.visibility = View.GONE
                binding.tvNoMeetings.visibility = View.VISIBLE
            }
        }

        // 읽지 않은 제안 표시
        viewModel.proposals.observe(viewLifecycleOwner) { proposals: List<MeetingProposal> ->
            if (proposals.isNotEmpty()) {
                val proposal = proposals.first()
                binding.cardProposal.visibility = View.VISIBLE
                binding.tvProposalTitle.text = proposal.title
                binding.tvProposalDate.text = "날짜: ${proposal.date}"
                binding.tvProposalTime.text = "시간: ${proposal.time}"
                binding.tvProposalLocation.text = "장소: ${proposal.location}"
                binding.tvNoProposals.visibility = View.GONE
            } else {
                binding.cardProposal.visibility = View.GONE
                binding.tvNoProposals.visibility = View.VISIBLE
            }
        }

        // 수락 버튼 클릭
        binding.btnAccept.setOnClickListener {
            viewModel.proposals.value?.firstOrNull()?.let { proposal ->
                animateCardAndRemove(proposal, isAccepted = true)
            }
        }

        // 거절 버튼 클릭
        binding.btnDecline.setOnClickListener {
            viewModel.proposals.value?.firstOrNull()?.let { proposal ->
                animateCardAndRemove(proposal, isAccepted = false)
            }
        }

        // 알림 버튼 클릭
        binding.ivNotification.setOnClickListener {
            if (parentFragmentManager.findFragmentByTag(NotificationFragment.TAG) == null) {
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, 0)
                    .add(R.id.main_frm, NotificationFragment(), NotificationFragment.TAG)
                    .commit()
            }
        }
    }

    // 시간표 렌더링 메서드 (30분 단위로 수정)
    private fun renderTimetable(entries: List<TimetableEntry>) {
        val timetableLayout = binding.flTimetable
        timetableLayout.removeAllViews()

        // 동적 시간 범위 계산
        var minHour = 9  // 기본 최소 시간 (9시)
        var maxHour = 20 // 기본 최대 시간 (20시)

        // 일정이 있는 경우 시간 범위 조정
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
                textSize = 13f // 더 크게 폰트 크기 증가
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#F5F5F5"))
                layoutParams = TableRow.LayoutParams().apply {
                    width = 0
                    height = TableRow.LayoutParams.WRAP_CONTENT
                    weight = 1f
                }
                setPadding(4, 10, 4, 10) // 패딩 더 추가
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

            // 시간 셀 - 정시(00분)에만 시간 표시
            val timeCell = TextView(requireContext()).apply {
                text = if (minute == 0) hour.toString() else ""
                textSize = 12f // 폰트 크기 증가
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#F5F5F5"))
                layoutParams = TableRow.LayoutParams().apply {
                    width = 40
                    height = 35 // 30분 단위이므로 높이 조정 (기존 65의 절반보다 약간 큰 값)
                }
            }
            row.addView(timeCell)

            // 요일별 셀
            for (day in 1 until weekDays.size) {
                // 현재 시간대의 일정 찾기 (30분 단위 고려)
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
                        height = 35
                        weight = 1f
                    }
                    gravity = Gravity.CENTER

                    if (entry != null) {
                        // 일정이 있는 경우
                        setBackgroundColor(Color.parseColor(entry.colorHex))
                        alpha = 0.85f

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

                        // 최소 길이 체크 (적어도 1시간 이상이어야 제목/장소 표시)
                        val isLongEnough = (endTimeInMinutes - startTimeInMinutes) >= 60

                        if (isLongEnough && isTitleCell) {
                            addView(TextView(requireContext()).apply {
                                text = entry.title
                                textSize = 11f
                                gravity = Gravity.CENTER
                                setTextColor(Color.WHITE)
                                ellipsize = android.text.TextUtils.TruncateAt.END
                                maxLines = 1
                                setPadding(2, 0, 2, 0)
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
                        }
                        // 1시간 미만인 경우 첫 번째 셀에 제목만 표시
                        else if (!isLongEnough && currentTimeInMinutes == startTimeInMinutes) {
                            addView(TextView(requireContext()).apply {
                                text = entry.title
                                textSize = 11f
                                gravity = Gravity.CENTER
                                setTextColor(Color.WHITE)
                                ellipsize = android.text.TextUtils.TruncateAt.END
                                maxLines = 1
                                setPadding(2, 0, 2, 0)
                            })
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

        // 필요하다면 시간표에 마진 추가
        (timetableLayout.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
            setMargins(16, 16, 16, 24)
        }
    }

    // 애니메이션 포함된 카드 제거 함수
    private fun animateCardAndRemove(proposal: MeetingProposal, isAccepted: Boolean) {
        binding.cardProposal.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (isAccepted) {
                    viewModel.acceptProposal(proposal)
                } else {
                    viewModel.declineProposal(proposal)
                }

                // 뷰 초기화
                binding.cardProposal.visibility = View.GONE
                binding.tvNoProposals.visibility = View.VISIBLE
                binding.cardProposal.alpha = 1f
                binding.cardProposal.translationY = 0f
            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}