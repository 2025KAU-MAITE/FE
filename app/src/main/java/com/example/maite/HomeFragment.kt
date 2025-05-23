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
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.example.maite.notification.NotificationViewModel
import com.example.maite.notification.NotificationViewModelFactory
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.example.maite.databinding.FragmentHomeBinding
import com.example.maite.data.model.MeetingItem
import com.example.maite.data.model.MeetingProposal
import com.example.maite.data.model.ProposalType
import com.example.maite.model.TimetableEntry
import com.example.maite.ui.home.HomeViewModel
import com.example.maite.ui.home.HomeViewModelFactory
import com.example.maite.ui.profile.ProfileViewModel
import kotlin.math.ceil
import android.util.Log
import android.os.Handler
import android.os.Looper
import com.example.maite.ChatListFragment
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ViewModel 초기화
    private val viewModel by viewModels<HomeViewModel> {
        HomeViewModelFactory(requireContext())
    }
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val preferencesUtil by lazy { PreferencesUtil(requireContext()) }
    
    // 알림 ViewModel 추가
    private val notificationViewModel by viewModels<NotificationViewModel> {
        NotificationViewModelFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 사용자 ID를 가져와서 시간표 로드
        val userId = preferencesUtil.getUserId()
        
        if (userId != null) {
            // 서버에서 시간표 로드
            lifecycleScope.launch {
                profileViewModel.loadTimetableFromServer(userId)
            }
            
            // 5초 후에도 시간표가 비어있으면 다시 로드 시도
            Handler(Looper.getMainLooper()).postDelayed({
                if (viewModel.timetableEntries.value?.isEmpty() == true) {
                    lifecycleScope.launch {
                        profileViewModel.loadTimetableFromServer(userId)
                    }
                }
            }, 5000)
        } else {
            
            // 사용자 ID가 없는 경우 로그인 필요 안내
            Snackbar.make(
                binding.root,
                "로그인이 필요합니다. 로그인 후 시간표를 확인할 수 있습니다.",
                Snackbar.LENGTH_LONG
            ).show()
        }
        
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
                
                // 회의방 초대인지 회의 제안인지 구분하여 표시 (디자인 개선)
                if (proposal.type == ProposalType.ROOM_INVITE) {
                    // 회의방 초대 표시
                    binding.tvProposalTitle.text = "${proposal.fromUser}님의 회의방 초대"
                    binding.tvProposalDate.text = "회의방: ${proposal.roomName ?: ""}"
                    binding.tvProposalTime.visibility = View.GONE
                    binding.tvProposalLocation.visibility = View.GONE
                    
                    // 회의방 초대의 경우에도 흰색 배경 사용
                    binding.cardProposal.setCardBackgroundColor(resources.getColor(R.color.white, null))
                    binding.ivInviteIcon.setImageResource(R.drawable.ic_room_invite)
                    binding.ivInviteIcon.visibility = View.VISIBLE
                } else {
                    // 회의 제안 표시
                    binding.tvProposalTitle.text = proposal.title
                    binding.tvProposalDate.text = "날짜: ${proposal.date ?: ""}"
                    binding.tvProposalTime.text = "시간: ${proposal.time ?: ""}"
                    binding.tvProposalLocation.text = "장소: ${proposal.location ?: ""}"
                    binding.tvProposalTime.visibility = View.VISIBLE
                    binding.tvProposalLocation.visibility = View.VISIBLE
                    
                    // 회의 제안의 경우 기본 카드 배경색 유지
                    binding.cardProposal.setCardBackgroundColor(resources.getColor(R.color.white, null))
                    binding.ivInviteIcon.setImageResource(R.drawable.ic_meeting_invite)
                    binding.ivInviteIcon.visibility = View.VISIBLE
                }
                
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
                    .add(R.id.main_frm, NotificationFragment(), NotificationFragment.TAG)
                    .commit()
            }
        }

        // 알림 개수 관찰 및 배지 업데이트
        notificationViewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            val notificationCount = notifications.size
            if (notificationCount > 0) {
                binding.notificationBadge.apply {
                    visibility = View.VISIBLE
                    text = if (notificationCount > 9) "9+" else notificationCount.toString()
                }
            } else {
                binding.notificationBadge.visibility = View.GONE
            }
        }
        
        // 알림 데이터 로드
        notificationViewModel.loadNotifications()
        
        // 회의방 참가 이벤트 관찰 (토스트 메시지 표시)
        viewModel.roomJoinEvent.observe(viewLifecycleOwner) { roomName ->
            if (roomName != null) {
                Toast.makeText(
                    requireContext(),
                    "'$roomName' 회의방에 참가하였습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                
                // 이벤트 처리후 초기화 - 직접 접근하지 않고 ViewModel의 메서드 사용
                try {
                    // 이벤트 처리후 초기화
                    viewModel.clearRoomJoinEvent()
                } catch (e: Exception) {
                    // 이벤트 초기화 실패 시 무시
                }
            }
        }
        
        // 회의방으로 이동 이벤트 관찰
        viewModel.navigateToRoomId.observe(viewLifecycleOwner) { roomId ->
            if (roomId != null) {
                // 바텀 네비게이션에서 List 탭으로 이동
                val mainActivity = activity as? MainActivity
                mainActivity?.navigateToListTab()
                
                // 로딩 시간을 주기 위해 약간의 딜레이 후 처리
                Handler(Looper.getMainLooper()).postDelayed({
                    // SharedPreferences나 앱 내 데이터 저장소에 방금 참가한 방 ID 저장
                    preferencesUtil.setLastJoinedRoomId(roomId)
                    
                    // 방금 수락한 방 ID 초기화 (중복 이동 방지)
                    viewModel.clearNavigateToRoomId()
                }, 300)
            }
        }
        
        // 로딩 상태 관찰
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // 오류 메시지 관찰
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                // JSON 파싱 오류인 경우 좀 더 사용자 친화적인 메시지로 변경
                val displayMessage = if (it.contains("malformed") || it.contains("JsonReader") || it.contains("parsing")) {
                    "서버와의 통신 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                } else {
                    it
                }
                
                Snackbar.make(binding.root, displayMessage, Snackbar.LENGTH_LONG)
                    .setAction("확인") {
                        viewModel.clearError()
                    }
                    .setActionTextColor(resources.getColor(R.color.mainColor, null))
                    .show()
            }
        }
    }

    // 시간표 렌더링 메서드 - 안전한 버전으로 수정
    private fun renderTimetable(entries: List<TimetableEntry>) {
        try {
            
            val timetableLayout = binding.flTimetable
            timetableLayout.removeAllViews()

            // 동적 시간 범위 계산
            var minHour = 8
            var maxHour = 24

            // 일정이 있는 경우 시간 범위 조정
            if (entries.isNotEmpty()) {
                val startTimes = entries.map { it.startHour + (it.startMinute / 60.0) }
                val endTimes = entries.map { it.endHour + (it.endMinute / 60.0) }

                startTimes.minOrNull()?.let { minStart ->
                    if (minStart.toInt() < minHour) {
                        minHour = minStart.toInt()
                    }
                }

                endTimes.maxOrNull()?.let { maxEnd ->
                    if (ceil(maxEnd).toInt() > maxHour) {
                        maxHour = ceil(maxEnd).toInt()
                    }
                }
            }

            // 시간 범위 제한
            minHour = minHour.coerceIn(0, 23)
            maxHour = maxHour.coerceIn(minHour + 1, 24)

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
                    textSize = 13f
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

            // 시간 행 추가 - 안전한 버전
            val hourCellHeight = try {
                resources.getDimensionPixelSize(R.dimen.timetable_cell_height)
            } catch (e: Exception) {
                120 // 기본값 사용
            }

            for (hour in minHour until maxHour) {
                val row = TableRow(requireContext())

                // 시간 표시 열
                val timeCell = TextView(requireContext()).apply {
                    text = hour.toString()
                    textSize = 12f
                    gravity = Gravity.CENTER
                    setBackgroundColor(Color.WHITE)
                    layoutParams = TableRow.LayoutParams().apply {
                        width = 40
                        height = hourCellHeight
                    }
                }
                row.addView(timeCell)

                // 요일별 셀
                for (day in 1 until weekDays.size) {
                    // 해당 시간에 해당하는 일정 찾기
                    val entriesInThisHour = entries.filter { e ->
                        val startTime = e.startHour * 60 + e.startMinute
                        val endTime = e.endHour * 60 + e.endMinute
                        val hourStart = hour * 60
                        val hourEnd = (hour + 1) * 60
                        
                        e.dayOfWeek == day && !(endTime <= hourStart || startTime >= hourEnd)
                    }

                    if (entriesInThisHour.isEmpty()) {
                        // 빈 셀 추가
                        val emptyCell = LinearLayout(requireContext()).apply {
                            layoutParams = TableRow.LayoutParams(0, hourCellHeight, 1f)
                            setBackgroundResource(R.drawable.timetable_cell_border)
                        }
                        row.addView(emptyCell)
                    } else {
                        // 일정이 있는 셀 생성
                        val entry = entriesInThisHour[0]
                        
                        val startTimeInMinutes = entry.startHour * 60 + entry.startMinute
                        val endTimeInMinutes = entry.endHour * 60 + entry.endMinute
                        val hourStartMinutes = hour * 60
                        val hourEndMinutes = (hour + 1) * 60
                        
                        val isStartHour = startTimeInMinutes >= hourStartMinutes && startTimeInMinutes < hourEndMinutes
                        val isEndHour = endTimeInMinutes > hourStartMinutes && endTimeInMinutes <= hourEndMinutes
                        
                        val cell = LinearLayout(requireContext()).apply {
                            val startRatio = if (startTimeInMinutes <= hourStartMinutes) 0f
                                           else (startTimeInMinutes - hourStartMinutes) / 60f
                            val endRatio = if (endTimeInMinutes >= hourEndMinutes) 1f
                                         else (endTimeInMinutes - hourStartMinutes) / 60f
                            
                            val topMargin = (hourCellHeight * startRatio).toInt()
                            val heightRatio = endRatio - startRatio
                            val cellContentHeight = (hourCellHeight * heightRatio).toInt()
                            
                            layoutParams = TableRow.LayoutParams(0, cellContentHeight, 1f).apply {
                                this.topMargin = topMargin
                            }
                            
                            gravity = Gravity.CENTER
                            orientation = LinearLayout.VERTICAL
                            // 순수한 색상만 사용 - 테두리 제거
                            setBackgroundColor(Color.parseColor(entry.colorHex))
                            
                            // 텍스트 표시 - 시간 정보 제거
                            if (isStartHour) {
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
                                addView(TextView(requireContext()).apply {
                                    text = "장소:${entry.location}"
                                    textSize = 9f
                                    gravity = Gravity.CENTER
                                    setTextColor(Color.WHITE)
                                    ellipsize = android.text.TextUtils.TruncateAt.END
                                    maxLines = 1
                                    setPadding(4, 0, 4, 0)
                                })
                            }
                        }
                        
                        row.addView(cell)
                    }
                }

                tableLayout.addView(row)
            }

            timetableLayout.addView(tableLayout)

            // 마진 추가 (안전하게)
            try {
                (timetableLayout.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                    setMargins(16, 16, 16, 24)
                }
            } catch (e: Exception) {
                // 마진 설정 실패 시 무시
            }
            
        } catch (e: Exception) {
            
            // 에러 발생 시 기본 메시지 표시
            val errorTextView = TextView(requireContext()).apply {
                text = "시간표를 불러오는 중 문제가 발생했습니다."
                gravity = Gravity.CENTER
                setPadding(16, 16, 16, 16)
            }
            binding.flTimetable.removeAllViews()
            binding.flTimetable.addView(errorTextView)
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
                    // UI 업데이트 진행중 표시
                    val loadingDialog = LoadingDialog(requireContext())
                    loadingDialog.show()
                    
                    // 제안 수락 처리 - 실제 API 호출
                    viewModel.acceptProposal(proposal)
                    
                    // 제안 유형에 따라 다른 처리
                    if (proposal.type == ProposalType.MEETING) {
                        // 회의 제안이면 회의 목록 갱신
                        // 잠시 대기 후 서버에서 업데이트 된 회의 목록을 가져오기
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            viewModel.loadNearestMeeting()
                            Log.d("HomeFragment", "회의 목록 새로 가져오기 완료")
                        }, 500) // 0.5초 대기 후 새로고침
                        
                        // 성공 메시지 표시
                        Toast.makeText(
                            requireContext(),
                            "회의 제안을 수락했습니다. 회의 목록을 업데이트합니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    // ROOM_INVITE인 경우는 roomJoinEvent로 처리됨 (별도의 토스트 메시지 관찰자로)
                    
                    // 잠시 후 로딩 닫기
                    loadingDialog.dismiss()
                } else {
                    // 제안 거절 처리 - 실제 API 호출
                    viewModel.declineProposal(proposal)
                    
                    // 거절 메시지 표시
                    Toast.makeText(
                        requireContext(),
                        "제안을 거절했습니다",
                        Toast.LENGTH_SHORT
                    ).show()
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