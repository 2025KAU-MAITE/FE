package com.example.maite

import android.content.Context
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.maite.databinding.FragmentListDetailBinding
import com.example.maite.model.InviteUserRequest
import com.example.maite.model.MaiteListItem
import com.example.maite.model.MeetingDataManager
import com.example.maite.viewmodel.InviteListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class ListDetailFragment : Fragment() {
    private var _binding: FragmentListDetailBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: TimeSelectionViewModel by activityViewModels()
    private lateinit var inviteViewModel: InviteListViewModel

    // 회의 데이터 관리자 추가
    private lateinit var meetingDataManager: MeetingDataManager

    private lateinit var apiService: MaiteApiService
    private var maiteListItem: MaiteListItem? = null

    private val weekDays = arrayOf("", "월", "화", "수", "목", "금", "토", "일")
    private val timeSlots = Array(25) { String.format("%02d", it) } // 00 ~ 24
    private var classes = listOf<TimetableItem>(
        // 초기 데이터는 API 응답으로 대체됨
    )

    private lateinit var availableDaysOfWeek: Set<Int>

    private var participantEmails: List<String> = emptyList()

    private var userEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListDetailBinding.inflate(inflater, container, false)
        // availableDaysOfWeek는 loadTimetableData 이후에 설정됩니다.
        // sharedViewModel.setTimetableData는 loadTimetableData 이후에 호출됩니다.

        apiService = MaiteRetrofitClient.getInstance(requireContext())

        // 회의 데이터 관리자 초기화
        meetingDataManager = MeetingDataManager(requireContext())

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

        participantEmails = maiteListItem?.participantEmails ?: emptyList()
        Log.d("ListDetailFragment", "참가자 이메일 목록: $participantEmails")

        updateParticipantProfiles()

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.addBtn.setOnClickListener {
            val userList = inviteViewModel.inviteList.value ?: emptyList()
            val selectedIds = userList
                .filter { item -> participantEmails.contains(item.email) }
                .map { it.id }
            Log.d("ListDetailFragment", "이메일로 매칭된 사용자 ID 목록: $selectedIds")
            val bottomSheet = InviteBottomSheet.newInstance(selectedIds, true)
            bottomSheet.show(parentFragmentManager, bottomSheet.tag)
        }

        parentFragmentManager.setFragmentResultListener(
            InviteBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val selectedCount = bundle.getInt(InviteBottomSheet.KEY_SELECTED_COUNT, 0)
            // val selectedIds = bundle.getSerializable(InviteBottomSheet.KEY_SELECTED_IDS) as? ArrayList<Long> // 사용 안함
            val selectedEmails = bundle.getStringArrayList(InviteBottomSheet.KEY_SELECTED_EMAILS) ?: arrayListOf()

            Log.d("ListDetailFragment", "선택된 참가자 수: $selectedCount, 이메일: $selectedEmails")

            val roomId = maiteListItem?.roomId
            if (roomId != null) {
                val newEmails = selectedEmails.filter { !participantEmails.contains(it) }
                if (newEmails.isNotEmpty()) {
                    inviteNewUsers(roomId, newEmails)
                }
            } else {
                Log.e("ListDetailFragment", "룸 ID가 null입니다.")
                Toast.makeText(requireContext(), "방 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }

            participantEmails = selectedEmails // 전체 참가자 목록 업데이트
            updateParticipantProfiles()
            loadTimetableData() // 참가자 변경 시 시간표 다시 로드
        }

        binding.timetableLayout.setOnClickListener {
            // availableDaysOfWeek는 loadTimetableData를 통해 업데이트되므로, 해당 시점의 값을 사용
            if (!::availableDaysOfWeek.isInitialized || availableDaysOfWeek.isEmpty()) {
                Log.d("ListDetailFragment", "SuggestBottomSheet: 사용 가능한 요일 정보가 아직 없거나 비어있습니다.")
                // 사용 가능한 요일이 없을 경우, 모든 요일을 전달하거나 사용자에게 알림
                val allDaysList = ArrayList((1..7).toList())
                
                // roomId와 inviteEmails 가져오기
                val roomId = maiteListItem?.roomId?.toInt() ?: -1
                val inviteEmails = ArrayList(participantEmails)
                
                if (roomId == -1) {
                    Toast.makeText(requireContext(), "방 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                if (inviteEmails.isEmpty()) {
                    Toast.makeText(requireContext(), "초대할 참가자가 없습니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val suggestBottomSheet = SuggestBottomSheet.newInstance(allDaysList, roomId, inviteEmails)
                suggestBottomSheet.show(parentFragmentManager, suggestBottomSheet.tag)
            } else {
                val availableDaysList = ArrayList(availableDaysOfWeek)
                Log.d("ListDetailFragment", "SuggestBottomSheet 생성, 전달 요일: $availableDaysList")
                
                // roomId와 inviteEmails 가져오기
                val roomId = maiteListItem?.roomId?.toInt() ?: -1
                val inviteEmails = ArrayList(participantEmails)
                
                if (roomId == -1) {
                    Toast.makeText(requireContext(), "방 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                if (inviteEmails.isEmpty()) {
                    Toast.makeText(requireContext(), "초대할 참가자가 없습니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                val suggestBottomSheet = SuggestBottomSheet.newInstance(availableDaysList, roomId, inviteEmails)
                suggestBottomSheet.show(parentFragmentManager, suggestBottomSheet.tag)
            }
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

        // 룸 ID가 있으면 회의 데이터 가져오기
        maiteListItem?.roomId?.let { roomId ->
            loadMeetingsData(roomId)
        }

        // 초기 시간표 데이터 로드 및 그리기
        loadTimetableData() // 이 시점에서 classes가 업데이트되고 createTimetable이 호출됨
    }

    /**
     * 회의 데이터를 가져오는 함수
     * @param roomId 회의 데이터를 가져올 방의 ID
     */
    private fun loadMeetingsData(roomId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 로딩 표시기 표시 (UI에 로딩 표시기가 있다면)
                // binding.meetingsProgressBar?.visibility = View.VISIBLE

                val success = meetingDataManager.fetchAndDistributeMeetings(roomId)

                if (success) {
                    // 데이터 로드 성공 - UI 업데이트
                    updateMeetingsUI()

                    Log.d("ListDetailFragment", "회의 데이터 로드 성공")
                } else {
                    Log.e("ListDetailFragment", "회의 데이터 로드 실패")
                    Toast.makeText(requireContext(), "회의 정보를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ListDetailFragment", "회의 데이터 로드 중 오류", e)
                Toast.makeText(requireContext(), "회의 데이터 로드 중 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                // 로딩 표시기 숨기기
                // binding.meetingsProgressBar?.visibility = View.GONE
            }
        }
    }

    /**
     * 회의 데이터 UI 업데이트
     */
    private fun updateMeetingsUI() {
        val pastMeetings = meetingDataManager.getMeetListRepository().getMeetList()
        val futureMeetings = meetingDataManager.getPropMeetRepository().getProposedMeetings()

        Log.d("ListDetailFragment", "과거 회의 수: ${pastMeetings.size}, 미래 회의 수: ${futureMeetings.size}")

        // 포맷터 설정 - yyyy-MM-dd 형식을 파싱하기 위함
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        try {
            // 가장 최근 과거 회의 찾기 (날짜 내림차순 정렬 후 첫번째 항목)
            if (pastMeetings.isNotEmpty()) {
                val latestPastMeeting = pastMeetings
                    .sortedByDescending {
                        try {
                            dateFormat.parse(it.date)?.time ?: 0
                        } catch (e: Exception) {
                            Log.e("ListDetailFragment", "날짜 파싱 오류: ${it.date}", e)
                            0 // 파싱 실패 시 가장 오래된 날짜로 처리
                        }
                    }
                    .firstOrNull()

                if (latestPastMeeting != null) {
                    Log.d("ListDetailFragment", "최근 회의: ${latestPastMeeting.title}, 날짜: ${latestPastMeeting.date}")

                    // UI 업데이트
                    binding.meetTitle.text = latestPastMeeting.title
                    binding.meetDate.text = formatDateForDisplay(latestPastMeeting.date)
                    binding.meetTime.text = latestPastMeeting.time
                    binding.meetPlace.text = latestPastMeeting.place

                    // 회의 카드 표시
                    binding.cardView3.visibility = View.VISIBLE
                } else {
                    Log.d("ListDetailFragment", "정렬 후 최근 회의가 없음")
                    binding.cardView3.visibility = View.GONE
                }
            } else {
                // 과거 회의가 없을 경우 UI 처리
                Log.d("ListDetailFragment", "최근 회의 없음")
                binding.cardView3.visibility = View.GONE
            }

            // 가장 가까운 미래 회의 찾기 (날짜 오름차순 정렬 후 첫번째 항목)
            if (futureMeetings.isNotEmpty()) {
                val earliestFutureMeeting = futureMeetings
                    .sortedBy {
                        try {
                            dateFormat.parse(it.date)?.time ?: Long.MAX_VALUE
                        } catch (e: Exception) {
                            Log.e("ListDetailFragment", "날짜 파싱 오류: ${it.date}", e)
                            Long.MAX_VALUE // 파싱 실패 시 가장 미래 날짜로 처리
                        }
                    }
                    .firstOrNull()

                if (earliestFutureMeeting != null) {
                    Log.d("ListDetailFragment",
                        "다가오는 회의: ${earliestFutureMeeting.title}, " +
                                "날짜: ${earliestFutureMeeting.date}, " +
                                "상태: ${earliestFutureMeeting.acceptance}")

                    // UI 업데이트
                    binding.propTitle.text = earliestFutureMeeting.title
                    binding.propDate.text = formatDateForDisplay(earliestFutureMeeting.date)
                    binding.propTime.text = earliestFutureMeeting.time
                    binding.propPlace.text = earliestFutureMeeting.place

                    // 회의 상태에 따라 UI 업데이트
                    when (earliestFutureMeeting.acceptance.uppercase()) {
                        "ACCEPTED" -> {
                            // 수락된 회의: 수락/거절 버튼 숨김, 상태 표시
                            binding.acceptBtn.visibility = View.GONE
                            binding.rejectBtn.visibility = View.GONE

                            // 상태 카드 표시
                            binding.status.visibility = View.VISIBLE
                            binding.statusBackground.setBackgroundResource(R.color.mainColor)
                            binding.statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                            binding.statusText.text = "수락됨"
                        }
                        "REJECTED" -> {
                            // 거절된 회의: 수락/거절 버튼 숨김, 상태 표시
                            binding.acceptBtn.visibility = View.GONE
                            binding.rejectBtn.visibility = View.GONE

                            // 상태 카드 표시
                            binding.status.visibility = View.VISIBLE
                            binding.statusBackground.setBackgroundResource(R.color.light_gray)
                            binding.statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                            binding.statusText.text = "거절됨"
                        }
                        else -> {
                            // PENDING 또는 다른 상태: 수락/거절 버튼 표시, 상태 숨김
                            binding.acceptBtn.visibility = View.VISIBLE
                            binding.rejectBtn.visibility = View.VISIBLE
                            binding.status.visibility = View.GONE

                            // 수락/거절 버튼 이벤트 설정
                            binding.acceptBtn.setOnClickListener {
                                val meetingId = earliestFutureMeeting.meetingId
                                acceptMeeting(meetingId)
                            }

                            binding.rejectBtn.setOnClickListener {
                                val meetingId = earliestFutureMeeting.meetingId
                                rejectMeeting(meetingId)
                            }
                        }
                    }

                    // 회의 카드 표시
                    binding.cardView2.visibility = View.VISIBLE
                } else {
                    Log.d("ListDetailFragment", "정렬 후 다가오는 회의가 없음")
                    binding.cardView2.visibility = View.GONE
                }
            } else {
                // 미래 회의가 없을 경우 UI 처리
                Log.d("ListDetailFragment", "다가오는 회의 없음")
                binding.cardView2.visibility = View.GONE
            }
        } catch (e: Exception) {
            Log.e("ListDetailFragment", "회의 데이터 UI 업데이트 중 오류", e)
            // 오류 발생 시 카드 숨기기
            binding.cardView2.visibility = View.GONE
            binding.cardView3.visibility = View.GONE
        }
    }

    private fun acceptMeeting(meetingId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 로딩 표시 (옵션)
                binding.acceptBtn.isEnabled = false
                binding.rejectBtn.isEnabled = false

                val response = withContext(Dispatchers.IO) {
                    apiService.acceptMeetingInvite(meetingId)
                }

                if (response.isSuccessful) {
                    Log.d("ListDetailFragment", "회의 수락 성공: 회의 ID $meetingId")
                    Toast.makeText(requireContext(), "회의를 수락했습니다.", Toast.LENGTH_SHORT).show()

                    // 회의 데이터 다시 로드하여 UI 업데이트
                    maiteListItem?.roomId?.let { roomId ->
                        loadMeetingsData(roomId)
                    }
                } else {
                    Log.e("ListDetailFragment", "회의 수락 실패: ${response.code()}")
                    Toast.makeText(requireContext(), "회의 수락 실패: 상태 코드 ${response.code()}", Toast.LENGTH_SHORT).show()

                    // 버튼 다시 활성화
                    binding.acceptBtn.isEnabled = true
                    binding.rejectBtn.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e("ListDetailFragment", "회의 수락 처리 중 오류", e)
                Toast.makeText(requireContext(), "회의 수락 처리 중 오류: ${e.message}", Toast.LENGTH_SHORT).show()

                // 버튼 다시 활성화
                binding.acceptBtn.isEnabled = true
                binding.rejectBtn.isEnabled = true
            }
        }
    }

    /**
     * 회의 초대 거절 처리
     * @param meetingId 거절할 회의 ID
     */
    private fun rejectMeeting(meetingId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 로딩 표시 (옵션)
                binding.acceptBtn.isEnabled = false
                binding.rejectBtn.isEnabled = false

                val response = withContext(Dispatchers.IO) {
                    apiService.rejectMeetingInvite(meetingId)
                }

                if (response.isSuccessful) {
                    Log.d("ListDetailFragment", "회의 거절 성공: 회의 ID $meetingId")
                    Toast.makeText(requireContext(), "회의를 거절했습니다.", Toast.LENGTH_SHORT).show()

                    // 회의 데이터 다시 로드하여 UI 업데이트
                    maiteListItem?.roomId?.let { roomId ->
                        loadMeetingsData(roomId)
                    }
                } else {
                    Log.e("ListDetailFragment", "회의 거절 실패: ${response.code()}")
                    Toast.makeText(requireContext(), "회의 거절 실패: 상태 코드 ${response.code()}", Toast.LENGTH_SHORT).show()

                    // 버튼 다시 활성화
                    binding.acceptBtn.isEnabled = true
                    binding.rejectBtn.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e("ListDetailFragment", "회의 거절 처리 중 오류", e)
                Toast.makeText(requireContext(), "회의 거절 처리 중 오류: ${e.message}", Toast.LENGTH_SHORT).show()

                // 버튼 다시 활성화
                binding.acceptBtn.isEnabled = true
                binding.rejectBtn.isEnabled = true
            }
        }
    }

    private fun formatDateForDisplay(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)

            if (date != null) {
                outputFormat.format(date)
            } else {
                dateString // 파싱 실패 시 원본 반환
            }
        } catch (e: Exception) {
            Log.e("ListDetailFragment", "날짜 포맷 변환 오류", e)
            dateString // 오류 발생 시 원본 반환
        }
    }

    private fun loadTimetableData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val currentParticipantEmails = participantEmails // 현재 시점의 참가자 목록 사용
                val totalParticipants = currentParticipantEmails.size
                Log.d("ListDetailFragment", "시간표 데이터 로드 시작. 참가자 수: $totalParticipants")

                val allUsersBusyHours = mutableListOf<Map<Int, Set<Int>>>()

                if (totalParticipants > 0) {
                    for (email in currentParticipantEmails) {
                        Log.d("ListDetailFragment", "사용자 $email 시간표 데이터 로드 중...")
                        try {
                            val response = withContext(Dispatchers.IO) {
                                apiService.getTimetableByEmail(email)
                            }
                            if (response.isSuccessful && response.body()?.isSuccess == true) {
                                val timetableResponse = response.body()!!
                                val userBusyHours = RoomTimetableUtils.convertToUserBusyHours(timetableResponse)
                                allUsersBusyHours.add(userBusyHours)
                                Log.d("ListDetailFragment", "사용자 $email 시간표 로드 성공")
                            } else {
                                val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
                                Log.e("ListDetailFragment", "사용자 $email 시간표 로드 실패: $errorBody")
                            }
                        } catch (e: Exception) {
                            Log.e("ListDetailFragment", "사용자 $email 시간표 로드 중 오류 발생", e)
                        }
                    }
                }

                val busyCounts = RoomTimetableUtils.calculateBusyCountsPerSlot(allUsersBusyHours)

                val allFreeColor = Color.parseColor("#4C7EED")
                val nMinusOneFreeColor = Color.parseColor("#A8C5F7") // 연한 파란색 (예시)

                val newClasses = RoomTimetableUtils.convertBusyCountsToTimetableItems(
                    busyCounts,
                    totalParticipants,
                    allFreeColor,
                    nMinusOneFreeColor
                )

                withContext(Dispatchers.Main) {
                    Log.d("ListDetailFragment", "시간표 항목 생성 완료. 항목 수: ${newClasses.size}")
                    classes = newClasses
                    availableDaysOfWeek = classes.mapNotNull { item ->
                        // N명 또는 N-1명 비는 시간만 availableDaysOfWeek에 포함
                        if (item.className.contains("비는 시간")) item.dayOfWeek else null
                    }.toSet()

                    Log.d("ListDetailFragment", "사용 가능한 요일 업데이트: $availableDaysOfWeek")

                    sharedViewModel.setTimetableData(classes) // ViewModel 업데이트
                    createTimetable() // 시간표 UI 다시 그리기

                    val message: String
                    if (totalParticipants > 0) {
                        val foundSlots = newClasses.any { it.className.contains("비는 시간") }
                        message = if (foundSlots) {
                            "${totalParticipants}명의 참가자 시간표를 분석했습니다."
                        } else {
                            "${totalParticipants}명의 참가자 시간표를 분석했지만, 공통 비는 시간을 찾지 못했습니다."
                        }
                    } else {
                        message = "참가자가 없습니다. 기본 시간표를 표시합니다."
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    Log.d("ListDetailFragment", "시간표 UI 업데이트 완료 및 토스트 메시지 표시: $message")
                }
            } catch (e: Exception) {
                Log.e("ListDetailFragment", "시간표 데이터 로드 중 심각한 오류 발생", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "시간표 로드 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                    // 오류 발생 시 기본 빈 시간표 표시
                    classes = emptyList()
                    availableDaysOfWeek = emptySet()
                    sharedViewModel.setTimetableData(classes)
                    createTimetable()
                }
            }
        }
    }

    private fun inviteNewUsers(roomId: Long, emails: List<String>) {
        viewLifecycleOwner.lifecycleScope.launch {
            var successCount = 0
            var failureDetails = ""
            try {
                for (email in emails) {
                    val request = InviteUserRequest(email)
                    val response = withContext(Dispatchers.IO) {
                        apiService.inviteUserToRoom(roomId, request)
                    }
                    if (response.isSuccessful) {
                        Log.d("ListDetailFragment", "사용자 초대 성공: $email")
                        successCount++
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("ListDetailFragment", "사용자 초대 실패 ($email): $errorBody")
                        if (failureDetails.isNotEmpty()) failureDetails += "\n"
                        failureDetails += "$email: $errorBody"
                    }
                }

                withContext(Dispatchers.Main) {
                    if (successCount > 0) {
                        Toast.makeText(requireContext(), "${successCount}명의 사용자를 성공적으로 초대했습니다.", Toast.LENGTH_SHORT).show()
                    }
                    if (failureDetails.isNotEmpty()) {
                        Toast.makeText(requireContext(), "일부 사용자 초대 실패:\n$failureDetails", Toast.LENGTH_LONG).show()
                    }
                    // 초대 후 참가자 목록이 변경되었으므로 시간표를 다시 로드할 수 있습니다.
                    // participantEmails는 BottomSheet 결과에서 이미 업데이트 되었으므로, loadTimetableData() 호출은 그쪽에서 처리합니다.
                }
            } catch (e: Exception) {
                Log.e("ListDetailFragment", "사용자 초대 중 오류 발생", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "초대 중 오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun updateParticipantProfiles() {
        val participantsLayout = binding.invitedUsersLayout
        val childrenToRemove = mutableListOf<View>()
        for (i in 0 until participantsLayout.childCount) {
            val child = participantsLayout.getChildAt(i)
            if (child is ImageView && child.id != R.id.addBtn) {
                childrenToRemove.add(child)
            }
        }
        childrenToRemove.forEach { participantsLayout.removeView(it) }

        val imageSize = resources.getDimensionPixelSize(R.dimen.invited_profile_img_size)
        val desiredMarginDp = 8
        val imageMarginEnd = (desiredMarginDp * resources.displayMetrics.density).toInt()

        // addBtn은 항상 마지막에 있도록 하기 위해, 참가자 프로필을 addBtn 이전에 추가합니다.
        val addBtn = participantsLayout.findViewById<ImageView>(R.id.addBtn)
        val addBtnIndex = if (addBtn != null) participantsLayout.indexOfChild(addBtn) else participantsLayout.childCount


        // 현재 participantEmails (최신 상태)를 사용
        for ((index, email) in participantEmails.withIndex()) {
            val imageView = ImageView(requireContext())
            val layoutParams = LinearLayout.LayoutParams(imageSize, imageSize)
            layoutParams.marginEnd = imageMarginEnd
            imageView.layoutParams = layoutParams

            Glide.with(requireContext())
                .load(R.drawable.img_profile_default)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView)

            imageView.tag = email
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.setOnClickListener {
                Toast.makeText(requireContext(), "참가자: $email", Toast.LENGTH_SHORT).show()
            }
            participantsLayout.addView(imageView, index) // addBtn 이전에 순차적으로 추가
        }

        binding.invitedUsersScrollView.post {
            binding.invitedUsersScrollView.fullScroll(View.FOCUS_RIGHT)
        }
    }

    private fun createTimetable() {
        val tableLayout = binding.root.findViewById<TableLayout>(R.id.timetableLayout)
        tableLayout.removeAllViews()

        val displayMinTime: Int
        val displayMaxTime: Int

        val relevantClasses = classes.filter { it.className.contains("비는 시간") }

        if (relevantClasses.isNotEmpty()) {
            displayMinTime = relevantClasses.minOfOrNull { it.timeSlot }?.let { (it - 1).coerceAtLeast(0) } ?: 0
            displayMaxTime = relevantClasses.maxOfOrNull { it.timeSlot }?.let { (it + 1).coerceAtMost(23) } ?: 23
        } else {
            displayMinTime = 0
            displayMaxTime = 23
            Log.w("ListDetailFragment", "표시할 비는 시간 데이터가 없어 기본 시간 범위($displayMinTime ~ $displayMaxTime) 사용")
        }
        Log.d("ListDetailFragment", "시간표 생성 범위: $displayMinTime 시 ~ $displayMaxTime 시")

        val timeColumnWidth = calculateTextWidth("00:00") + 24
        val cellHeight = resources.getDimensionPixelSize(R.dimen.timetable_cell_height)

        val ABSOLUTE_DAY_START_HOUR = 0
        val ABSOLUTE_DAY_END_HOUR = 23

        val headerRow = TableRow(context)
        val headerParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
        headerRow.layoutParams = headerParams
        val timeHeaderCell = TextView(context).apply {
            text = ""
            layoutParams = TableRow.LayoutParams(timeColumnWidth, TableRow.LayoutParams.WRAP_CONTENT)
            setBackgroundColor(Color.WHITE)
            setPadding(8, 12, 8, 12)
        }
        headerRow.addView(timeHeaderCell)
        for (i in 1 until weekDays.size) {
            val dayHeaderCell = TextView(context).apply {
                text = weekDays[i]
                gravity = Gravity.CENTER
                textSize = 12f
                setBackgroundColor(Color.WHITE)
                setPadding(4, 12, 4, 12)
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            }
            headerRow.addView(dayHeaderCell)
        }
        tableLayout.addView(headerRow)

        val timeRanges = findConsecutiveTimeRanges(displayMinTime, displayMaxTime)

        for (range in timeRanges) {
            val isAbsoluteEdgeStart = range.first == ABSOLUTE_DAY_START_HOUR
            val isAbsoluteEdgeEnd = range.second == ABSOLUTE_DAY_END_HOUR

            if (isAbsoluteEdgeStart || isAbsoluteEdgeEnd) {
                val row = TableRow(context)
                val rowParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
                row.layoutParams = rowParams

                // 표준 셀 2개 높이로 고정
                val actualRowHeight = 2 * cellHeight

                // --- 수정된 시간 셀 텍스트 생성 (세로 말줄임표 '⋮' 사용) ---
                val timeRangeText: String
                if (isAbsoluteEdgeStart) { // 상단 블록
                    val endTimeToDisplay = timeSlots.getOrNull(range.second + 1) ?: timeSlots.getOrNull(range.second)
                    timeRangeText = "⋮\n$endTimeToDisplay" // 세로 말줄임표 사용
                } else { // 하단 블록 (isAbsoluteEdgeEnd가 true여야 함)
                    val startTimeToDisplay = timeSlots.getOrNull(range.first) ?: ""
                    timeRangeText = "$startTimeToDisplay\n⋮" // 세로 말줄임표 사용
                }

                val timeCell = TextView(context).apply {
                    text = timeRangeText
                    gravity = Gravity.CENTER
                    textSize = 10f
                    setBackgroundColor(Color.WHITE)
                    setPadding(8, 8, 8, 8)
                    minLines = 2 // 2줄 텍스트를 위해
                    layoutParams = TableRow.LayoutParams(timeColumnWidth, actualRowHeight)
                }
                row.addView(timeCell)

                for (day in 1 until weekDays.size) {
                    val containerView = LinearLayout(context).apply {
                        layoutParams = TableRow.LayoutParams(0, actualRowHeight, 1f)
                        gravity = Gravity.CENTER
                        setBackgroundResource(R.drawable.timetable_cell_border)
                        orientation = LinearLayout.VERTICAL
                    }

                    val classItemForColor = classes.find { it.timeSlot == range.first && it.dayOfWeek == day && it.className.contains("비는 시간") }
                    if (classItemForColor != null) {
                        containerView.setBackgroundColor(classItemForColor.color)
                        val classNamesInRange = classes.filter { it.dayOfWeek == day && it.timeSlot >= range.first && it.timeSlot <= range.second }
                            .map { it.className }.distinct().joinToString(", ")
                        containerView.setOnLongClickListener {
                            Toast.makeText(context, classNamesInRange, Toast.LENGTH_SHORT).show()
                            true
                        }
                        containerView.setOnClickListener {
                            binding.timetableLayout.performClick()
                        }

                        // 세로 테두리를 위한 View 추가
                        val verticalLine = View(context)
                        val lineParams = LinearLayout.LayoutParams(
                            1.dpToPx(context), // 1dp 너비의 선
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        lineParams.gravity = Gravity.END // 오른쪽에 배치
                        verticalLine.layoutParams = lineParams
                        verticalLine.setBackgroundColor(Color.parseColor("#BBBBBB")) // 테두리 색상
                        verticalLine.alpha = 0.5f  // 테두리 투명도 조절
                        containerView.addView(verticalLine)
                    }
                    row.addView(containerView)
                }
                tableLayout.addView(row)

            } else {
                // --- 확장: 이 중간 범위에 대해 여러 개의 단일 시간 행 생성 ---
                for (hourInMiddleRange in range.first..range.second) {
                    val singleHourRow = TableRow(context)
                    val rowParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
                    singleHourRow.layoutParams = rowParams

                    val timeCellText = timeSlots.getOrNull(hourInMiddleRange) ?: ""
                    val timeCell = TextView(context).apply {
                        text = timeCellText
                        gravity = Gravity.CENTER
                        textSize = 10f
                        setBackgroundColor(Color.WHITE)
                        setPadding(8, 8, 8, 8)
                        layoutParams = TableRow.LayoutParams(timeColumnWidth, cellHeight)
                    }
                    singleHourRow.addView(timeCell)

                    for (day in 1 until weekDays.size) {
                        val containerView = LinearLayout(context).apply {
                            layoutParams = TableRow.LayoutParams(0, cellHeight, 1f)
                            gravity = Gravity.CENTER
                            setBackgroundResource(R.drawable.timetable_cell_border)
                            orientation = LinearLayout.VERTICAL
                        }

                        val classItemForColor = classes.find { it.timeSlot == hourInMiddleRange && it.dayOfWeek == day && it.className.contains("비는 시간") }
                        if (classItemForColor != null) {
                            containerView.setBackgroundColor(classItemForColor.color)
                            val classNameForSlot = classItemForColor.className
                            containerView.setOnLongClickListener {
                                Toast.makeText(context, classNameForSlot, Toast.LENGTH_SHORT).show()
                                true
                            }
                            containerView.setOnClickListener {
                                binding.timetableLayout.performClick()
                            }

                            // 세로 테두리를 위한 View 추가
                            val verticalLine = View(context)
                            val lineParams = LinearLayout.LayoutParams(
                                1.dpToPx(context), // 1dp 너비의 선
                                LinearLayout.LayoutParams.MATCH_PARENT
                            )
                            lineParams.gravity = Gravity.END // 오른쪽에 배치
                            verticalLine.layoutParams = lineParams
                            verticalLine.setBackgroundColor(Color.parseColor("#BBBBBB")) // 테두리 색상
                            verticalLine.alpha = 0.5f  // 테두리 투명도 조절
                            containerView.addView(verticalLine)
                        }
                        singleHourRow.addView(containerView)
                    }
                    tableLayout.addView(singleHourRow)
                }
            }
        }
    }

    private fun Int.dpToPx(context: Context?): Int {
        if (context == null) return this
        return (this * context.resources.displayMetrics.density).toInt()
    }


    private fun findConsecutiveTimeRanges(minTime: Int, maxTime: Int): List<Pair<Int, Int>> {
        if (minTime > maxTime) return emptyList() // 유효하지 않은 범위 처리

        val ranges = mutableListOf<Pair<Int, Int>>()
        // 시간대별로 모든 요일에 대한 클래스 상태(색상) 맵 생성
        // 색상이 다르면 다른 패턴으로 간주
        val timeStatusMap = mutableMapOf<Int, MutableMap<Int, Int?>>() // time -> (day -> colorHash)
        for (time in minTime..maxTime) {
            timeStatusMap[time] = mutableMapOf()
            for (day in 1..7) {
                // 해당 시간, 요일에 "비는 시간" 클래스 아이템의 색상을 가져옴. 없으면 null.
                timeStatusMap[time]!![day] = classes.find { it.timeSlot == time && it.dayOfWeek == day && it.className.contains("비는 시간") }?.color
            }
        }

        var rangeStart = minTime
        var currentPattern = timeStatusMap[minTime]

        for (time in (minTime + 1)..maxTime) {
            val nextPattern = timeStatusMap[time]
            if (nextPattern != currentPattern) {
                ranges.add(Pair(rangeStart, time - 1))
                rangeStart = time
                currentPattern = nextPattern
            }
        }
        // 마지막 범위 추가
        ranges.add(Pair(rangeStart, maxTime))

        Log.d("ListDetailFragment", "연속된 시간 범위 계산 완료: $ranges")
        return ranges
    }


    private fun calculateTextWidth(text: String): Int {
        val currentContext = context ?: return 0
        val textView = TextView(currentContext).apply {
            this.text = text
            this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f) // 시간표의 시간 텍스트 크기와 동일하게
            this.measure(
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

    data class TimetableItem(
        val timeSlot: Int,
        val dayOfWeek: Int,
        val className: String,
        val color: Int
    )

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