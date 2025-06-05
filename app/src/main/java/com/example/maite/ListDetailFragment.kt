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
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.example.maite.databinding.FragmentListDetailBinding
import com.example.maite.model.InviteUserRequest
import com.example.maite.model.MaiteListItem
import com.example.maite.model.MeetingDataManager
import com.example.maite.model.MeetingResponse
import com.example.maite.UserResult
import com.example.maite.model.ChatListItem
import com.example.maite.view.ChatRoomFragment
import com.example.maite.viewmodel.InviteListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.maite.model.ChatListRepository
import com.example.maite.model.CreateGroupChatRequest


class ListDetailFragment : Fragment() {
    private var _binding: FragmentListDetailBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: TimeSelectionViewModel by activityViewModels()
    private lateinit var inviteViewModel: InviteListViewModel

    private lateinit var meetingDataManager: MeetingDataManager
    private lateinit var apiService: MaiteApiService
    private var maiteListItem: MaiteListItem? = null

    private val weekDays = arrayOf("", "월", "화", "수", "목", "금", "토", "일")
    private val timeSlots = Array(25) { String.format("%02d", it) }
    private var classes = listOf<TimetableItem>()

    private lateinit var availableDaysOfWeek: Set<Int>
    private var participantEmails: List<String> = emptyList()
    private var userEmail: String? = null

    private lateinit var preferencesUtil: PreferencesUtil


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListDetailBinding.inflate(inflater, container, false)
        apiService = MaiteRetrofitClient.getInstance(requireContext())
        meetingDataManager = MeetingDataManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesUtil = PreferencesUtil(requireContext())

        inviteViewModel = ViewModelProvider(this)[InviteListViewModel::class.java]
        inviteViewModel.inviteList.observe(viewLifecycleOwner) { userList ->
        }

        maiteListItem = arguments?.getParcelable<MaiteListItem>(ARG_MAITE_LIST_ITEM)

        binding.title.text = maiteListItem?.title
        binding.intro.text = maiteListItem?.intro

        binding.chatBtn.setOnClickListener {
            navigateToChatRoom()
        }

        participantEmails = maiteListItem?.participantEmails ?: emptyList()

        updateParticipantProfiles()

        binding.backBtn.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.addBtn.setOnClickListener {
            val userList = inviteViewModel.inviteList.value ?: emptyList()
            val selectedIds = userList
                .filter { item -> participantEmails.contains(item.email) }
                .map { it.id }
            val bottomSheet = InviteBottomSheet.newInstance(selectedIds, true)
            bottomSheet.show(parentFragmentManager, bottomSheet.tag)
        }

        parentFragmentManager.setFragmentResultListener(
            InviteBottomSheet.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val selectedCount = bundle.getInt(InviteBottomSheet.KEY_SELECTED_COUNT, 0)
            val selectedEmails = bundle.getStringArrayList(InviteBottomSheet.KEY_SELECTED_EMAILS) ?: arrayListOf()

            val roomId = maiteListItem?.roomId
            if (roomId != null) {
                val newEmails = selectedEmails.filter { !participantEmails.contains(it) }
                if (newEmails.isNotEmpty()) {
                    inviteNewUsers(roomId, newEmails)
                }
            } else {
            }

            participantEmails = ArrayList(selectedEmails)
            updateParticipantProfiles()
            loadTimetableData()
        }

        binding.timetableLayout.setOnClickListener {
            if (!::availableDaysOfWeek.isInitialized || availableDaysOfWeek.isEmpty()) {
                val allDaysList = ArrayList((1..7).toList())
                val roomId = maiteListItem?.roomId?.toInt() ?: -1
                val inviteEmails = ArrayList(participantEmails)
                if (roomId == -1) {
                    return@setOnClickListener
                }
                if (inviteEmails.isEmpty()) {
                    return@setOnClickListener
                }
                val suggestBottomSheet = SuggestBottomSheet.newInstance(allDaysList, roomId, inviteEmails)
                suggestBottomSheet.show(parentFragmentManager, suggestBottomSheet.tag)
            } else {
                val availableDaysList = ArrayList(availableDaysOfWeek)
                val roomId = maiteListItem?.roomId?.toInt() ?: -1
                val inviteEmails = ArrayList(participantEmails)
                if (roomId == -1) {
                    return@setOnClickListener
                }
                if (inviteEmails.isEmpty()) {
                    return@setOnClickListener
                }
                val suggestBottomSheet = SuggestBottomSheet.newInstance(availableDaysList, roomId, inviteEmails)
                suggestBottomSheet.show(parentFragmentManager, suggestBottomSheet.tag)
            }
        }

        binding.propHamberger.setOnClickListener {
            val fragmentTag = PropMeetFragment::class.java.name
            val fragmentManager = parentFragmentManager
            var propMeetFragment = fragmentManager.findFragmentByTag(fragmentTag)

            val transaction = fragmentManager.beginTransaction()

            // 애니메이션 설정
            transaction.setCustomAnimations(
                R.anim.slide_in_right,
                0,
                0,
                R.anim.slide_out_right
            )

            if (propMeetFragment == null) {
                propMeetFragment = PropMeetFragment()
                transaction.add(R.id.main_frm, propMeetFragment, fragmentTag)
            } else {
                transaction.show(propMeetFragment)
            }

            transaction.addToBackStack(fragmentTag)
            transaction.commit()
        }

        binding.recentHamberger.setOnClickListener {
            val fragmentTag = MeetListFragment::class.java.name
            val fragmentManager = parentFragmentManager
            var meetListFragment = fragmentManager.findFragmentByTag(fragmentTag)

            val transaction = fragmentManager.beginTransaction()

            transaction.setCustomAnimations(
                R.anim.slide_in_right,
                0,
                0,
                R.anim.slide_out_right
            )

            if (meetListFragment == null) {
                meetListFragment = MeetListFragment()
                transaction.add(R.id.main_frm, meetListFragment, fragmentTag)
            } else {
                transaction.show(meetListFragment)
            }

            transaction.addToBackStack(fragmentTag)
            transaction.commit()
        }

        maiteListItem?.roomId?.let { roomId ->
            loadMeetingsData(roomId)
        }
        loadTimetableData()
    }

    private fun loadMeetingsData(roomId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val success = meetingDataManager.fetchAndDistributeMeetings(roomId)
                if (success) {
                    updateMeetingsUI()
                } else {
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun navigateToChatRoom() {
        // MAITE 항목이 유효한지 확인
        val maiteItem = maiteListItem ?: return
        val maiteTitle = maiteItem.title // 회의방 이름 (예: "KAU")

        // 로딩 표시 (실제 코드에선 ProgressBar 등으로 대체 권장)
        val loadingDialog = android.app.AlertDialog.Builder(requireContext())
            .setMessage("채팅방을 확인하는 중...")
            .setCancelable(false)
            .show()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. 채팅방 목록 가져오기
                val chatRepository = ChatListRepository(requireContext())
                val allChatRooms = withContext(Dispatchers.IO) {
                    chatRepository.getChatRooms()
                }

                // 2. 회의방과 동일한 이름을 가진 채팅방 찾기
                val matchingChatRoom = allChatRooms.find { chatRoom ->
                    chatRoom.name.equals(maiteTitle, ignoreCase = true)
                }

                if (matchingChatRoom != null) {
                    // 3a. 일치하는 채팅방이 있으면 해당 채팅방으로 이동
                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()
                        navigateToChatRoomWithItem(matchingChatRoom)
                    }
                } else {
                    // 3b. 일치하는 채팅방이 없으면 새 채팅방 생성
                    // 먼저 참여자 ID 목록 가져오기
                    val memberIds = mutableListOf<Long>()
                    val userId = preferencesUtil.getUserId() ?: -1L
                    memberIds.add(userId) // 현재 사용자 추가

                    // 현재 회의방의 참여자들 ID 가져오기
                    for (email in participantEmails) {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                apiService.searchUsers(email)
                            }
                            if (response.isSuccessful && response.body()?.isSuccess == true) {
                                val user = response.body()?.result?.firstOrNull { it.email == email }
                                if (user != null && !memberIds.contains(user.id)) {
                                    memberIds.add(user.id)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ListDetailFragment", "사용자 검색 오류: $email", e)
                        }
                    }

                    // 채팅방 생성 요청
                    val request = CreateGroupChatRequest(
                        roomName = maiteTitle,
                        memberIds = memberIds,
                        profileImageUrl = null
                    )

                    val createResponse = withContext(Dispatchers.IO) {
                        apiService.createGroupChatRoom(request)
                    }

                    withContext(Dispatchers.Main) {
                        loadingDialog.dismiss()

                        if (createResponse.isSuccessful && createResponse.body()?.isSuccess == true) {
                            // 생성된 채팅방 정보로 ChatListItem 생성
                            val result = createResponse.body()?.result
                            val newChatItem = ChatListItem(
                                id = result?.id.toString(),
                                name = result?.roomName ?: maiteTitle,
                                profileImageUrl = result?.profileImageUrl,
                                lastMessage = result?.lastMessageContent,
                                intro = "참여자 ${result?.participantCount ?: memberIds.size}명",
                                timestamp = System.currentTimeMillis(),
                                isGroup = true
                            )

                            // 생성된 채팅방으로 이동
                            navigateToChatRoomWithItem(newChatItem)
                        } else {
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun navigateToChatRoomWithItem(chatItem: ChatListItem) {
        val chatRoomFragment = ChatRoomFragment.newInstance(chatItem)
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                0,
                0,
                R.anim.slide_out_right
            )
            .add(R.id.main_frm, chatRoomFragment, ChatRoomFragment.TAG)
            .addToBackStack(null)
            .commit()
    }

    private fun updateMeetingsUI() {
        val pastMeetings = meetingDataManager.getMeetListRepository().getMeetList()
        val futureMeetings = meetingDataManager.getPropMeetRepository().getProposedMeetings()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        try {
            if (pastMeetings.isNotEmpty()) {
                val latestPastMeeting = pastMeetings
                    .sortedByDescending {
                        try { dateFormat.parse(it.date)?.time ?: 0 } catch (e: Exception) { 0 }
                    }
                    .firstOrNull()

                if (latestPastMeeting != null) {
                    binding.meetTitle.text = latestPastMeeting.title
                    binding.meetDate.text = formatDateForDisplay(latestPastMeeting.date)
                    binding.meetTime.text = "${latestPastMeeting.time} - ${latestPastMeeting.endTime}"
                    binding.meetPlace.text = latestPastMeeting.place
                    binding.cardView3.visibility = View.VISIBLE

                    binding.cardView3.setOnClickListener {
                        val meetingId = latestPastMeeting.meetingId
                        val meetDetailFragment = MeetDetailFragment.newInstance(meetingId)

                        parentFragmentManager.beginTransaction()
                            .setCustomAnimations(
                                R.anim.slide_in_right,
                                0,
                                0,
                                R.anim.slide_out_right
                            )
                            .add(R.id.main_frm, meetDetailFragment, MeetDetailFragment::class.java.name)
                            .addToBackStack(MeetDetailFragment::class.java.name)
                            .commit()
                    }
                } else {
                    binding.cardView3.visibility = View.GONE
                    binding.cardView3.setOnClickListener(null)
                }
            } else {
                binding.cardView3.visibility = View.GONE
                binding.cardView3.setOnClickListener(null)
            }

            if (futureMeetings.isNotEmpty()) {
                val earliestFutureMeeting = futureMeetings
                    .sortedBy {
                        try { dateFormat.parse(it.date)?.time ?: Long.MAX_VALUE } catch (e: Exception) { Long.MAX_VALUE }
                    }
                    .firstOrNull()

                if (earliestFutureMeeting != null) {
                    binding.propTitle.text = earliestFutureMeeting.title
                    binding.propDate.text = formatDateForDisplay(earliestFutureMeeting.date)
                    binding.propTime.text = "${earliestFutureMeeting.time} - ${earliestFutureMeeting.endTime}"
                    binding.propPlace.text = earliestFutureMeeting.place

                    when (earliestFutureMeeting.acceptance.uppercase()) {
                        "ACCEPTED" -> {
                            binding.acceptBtn.visibility = View.GONE
                            binding.rejectBtn.visibility = View.GONE
                            binding.status.visibility = View.VISIBLE
                            binding.statusBackground.setBackgroundResource(R.color.mainColor)
                            binding.statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                            binding.statusText.text = "수락됨"
                        }
                        "REJECTED" -> {
                            binding.acceptBtn.visibility = View.GONE
                            binding.rejectBtn.visibility = View.GONE
                            binding.status.visibility = View.VISIBLE
                            binding.statusBackground.setBackgroundResource(R.color.light_gray)
                            binding.statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                            binding.statusText.text = "거절됨"
                        }
                        else -> {
                            binding.acceptBtn.visibility = View.VISIBLE
                            binding.rejectBtn.visibility = View.VISIBLE
                            binding.status.visibility = View.GONE
                            binding.acceptBtn.setOnClickListener { acceptMeeting(earliestFutureMeeting.meetingId) }
                            binding.rejectBtn.setOnClickListener { rejectMeeting(earliestFutureMeeting.meetingId) }
                        }
                    }
                    binding.cardView2.visibility = View.VISIBLE
                    binding.cardView2.setOnClickListener {
                        val meetingId = earliestFutureMeeting.meetingId
                        val meetDetailFragment = MeetDetailFragment.newInstance(meetingId)

                        parentFragmentManager.beginTransaction()
                            .setCustomAnimations(
                                R.anim.slide_in_right,
                                0,
                                0,
                                R.anim.slide_out_right
                            )
                            .add(R.id.main_frm, meetDetailFragment, MeetDetailFragment::class.java.name)
                            .addToBackStack(MeetDetailFragment::class.java.name)
                            .commit()
                    }

                } else {
                    binding.cardView2.visibility = View.GONE
                    binding.cardView2.setOnClickListener(null)
                }
            } else {
                binding.cardView2.visibility = View.GONE
                binding.cardView2.setOnClickListener(null)
            }
        } catch (e: Exception) {
            binding.cardView2.visibility = View.GONE
            binding.cardView3.visibility = View.GONE
            binding.cardView2.setOnClickListener(null)
            binding.cardView3.setOnClickListener(null)
        }
    }

    private fun acceptMeeting(meetingId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.acceptBtn.isEnabled = false
                binding.rejectBtn.isEnabled = false
                val response = withContext(Dispatchers.IO) { apiService.acceptMeetingInvite(meetingId) }
                if (response.isSuccessful) {
                    maiteListItem?.roomId?.let { loadMeetingsData(it) }
                } else {
                    binding.acceptBtn.isEnabled = true
                    binding.rejectBtn.isEnabled = true
                }
            } catch (e: Exception) {
                binding.acceptBtn.isEnabled = true
                binding.rejectBtn.isEnabled = true
            }
        }
    }

    private fun rejectMeeting(meetingId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.acceptBtn.isEnabled = false
                binding.rejectBtn.isEnabled = false
                val response = withContext(Dispatchers.IO) { apiService.rejectMeetingInvite(meetingId) }
                if (response.isSuccessful) {
                    maiteListItem?.roomId?.let { loadMeetingsData(it) }
                } else {
                    binding.acceptBtn.isEnabled = true
                    binding.rejectBtn.isEnabled = true
                }
            } catch (e: Exception) {
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
            if (date != null) outputFormat.format(date) else dateString
        } catch (e: Exception) {
            dateString
        }
    }

    private fun loadTimetableData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val currentParticipantEmails = participantEmails
                val totalParticipants = currentParticipantEmails.size
                val allUsersBusyHours = mutableListOf<Map<Int, Set<Int>>>()
                if (totalParticipants > 0) {
                    for (email in currentParticipantEmails) {
                        try {
                            val response = withContext(Dispatchers.IO) { apiService.getTimetableByEmail(email) }
                            if (response.isSuccessful && response.body()?.isSuccess == true) {
                                val timetableResponse = response.body()!!
                                allUsersBusyHours.add(RoomTimetableUtils.convertToUserBusyHours(timetableResponse))
                            } else {
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
                val busyCounts = RoomTimetableUtils.calculateBusyCountsPerSlot(allUsersBusyHours)
                val newClasses = RoomTimetableUtils.convertBusyCountsToTimetableItems(
                    busyCounts, totalParticipants, Color.parseColor("#4C7EED"), Color.parseColor("#A8C5F7")
                )
                withContext(Dispatchers.Main) {
                    classes = newClasses
                    availableDaysOfWeek = classes.mapNotNull { item ->
                        if (item.className.contains("비는 시간")) item.dayOfWeek else null
                    }.toSet()
                    sharedViewModel.setTimetableData(classes)
                    createTimetable()
                    val message = if (totalParticipants > 0) {
                        if (newClasses.any { it.className.contains("비는 시간") }) "${totalParticipants}명의 참가자 시간표를 분석했습니다."
                        else "${totalParticipants}명의 참가자 시간표를 분석했지만, 공통 비는 시간을 찾지 못했습니다."
                    } else "참가자가 없습니다. 기본 시간표를 표시합니다."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
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
                    val response = withContext(Dispatchers.IO) { apiService.inviteUserToRoom(roomId, request) }
                    if (response.isSuccessful) {
                        successCount++
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        if (failureDetails.isNotEmpty()) failureDetails += "\n"
                        failureDetails += "$email: $errorBody"
                    }
                }
                withContext(Dispatchers.Main) {
                    if (successCount > 0) {
                    }
                    if (failureDetails.isNotEmpty()) {
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
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
        val imageMarginEnd = (8 * resources.displayMetrics.density).toInt()

        for ((index, email) in participantEmails.withIndex()) {
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(imageSize, imageSize).apply { marginEnd = imageMarginEnd }
                scaleType = ImageView.ScaleType.CENTER_CROP
                tag = email
                setOnClickListener {
                }
            }
            participantsLayout.addView(imageView, index)

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        apiService.searchUsers(email)
                    }
                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        val userList: List<UserResult>? = response.body()?.result
                        val userInfo: UserResult? = userList?.firstOrNull { it.email == email } ?: userList?.firstOrNull()
                        val profileImageUrl = userInfo?.profileImageUrl

                        if (!profileImageUrl.isNullOrBlank()) {
                            Glide.with(this@ListDetailFragment)
                                .load(profileImageUrl)
                                .apply(RequestOptions.circleCropTransform())
                                .skipMemoryCache(true)
                                .signature(ObjectKey(System.currentTimeMillis().toString()))
                                .placeholder(R.drawable.img_profile_default)
                                .error(R.drawable.img_profile_default)
                                .into(imageView)
                        } else {
                            imageView.setImageResource(R.drawable.img_profile_default)
                        }
                    } else {
                        imageView.setImageResource(R.drawable.img_profile_default)
                    }
                } catch (e: Exception) {
                    imageView.setImageResource(R.drawable.img_profile_default)
                }
            }
        }
        binding.invitedUsersScrollView.post { binding.invitedUsersScrollView.fullScroll(View.FOCUS_RIGHT) }
    }


    private fun createTimetable() {
        val tableLayout = binding.root.findViewById<TableLayout>(R.id.timetableLayout)
        tableLayout.removeAllViews()

        val relevantClasses = classes.filter { it.className.contains("비는 시간") }
        val displayMinTime = relevantClasses.minOfOrNull { it.timeSlot }?.let { (it - 1).coerceAtLeast(0) } ?: 0
        val displayMaxTime = relevantClasses.maxOfOrNull { it.timeSlot }?.let { (it + 1).coerceAtMost(23) } ?: 23

        val timeColumnWidth = calculateTextWidth("00:00") + 24
        val cellHeight = resources.getDimensionPixelSize(R.dimen.timetable_cell_height)
        val ABSOLUTE_DAY_START_HOUR = 0
        val ABSOLUTE_DAY_END_HOUR = 23

        val headerRow = TableRow(context).apply {
            layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
            addView(TextView(context).apply {
                layoutParams = TableRow.LayoutParams(timeColumnWidth, TableRow.LayoutParams.WRAP_CONTENT)
                setBackgroundColor(Color.WHITE)
                setPadding(8, 12, 8, 12)
            })
            for (i in 1 until weekDays.size) {
                addView(TextView(context).apply {
                    text = weekDays[i]; gravity = Gravity.CENTER; textSize = 12f
                    setBackgroundColor(Color.WHITE); setPadding(4, 12, 4, 12)
                    layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
                })
            }
        }
        tableLayout.addView(headerRow)

        val timeRanges = findConsecutiveTimeRanges(displayMinTime, displayMaxTime)
        for (range in timeRanges) {
            val isAbsoluteEdgeStart = range.first == ABSOLUTE_DAY_START_HOUR
            val isAbsoluteEdgeEnd = range.second == ABSOLUTE_DAY_END_HOUR

            if (isAbsoluteEdgeStart || isAbsoluteEdgeEnd) {
                val row = TableRow(context).apply { layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT) }
                val actualRowHeight = 2 * cellHeight
                val timeRangeText = if (isAbsoluteEdgeStart) "⋮\n${timeSlots.getOrNull(range.second + 1) ?: timeSlots.getOrNull(range.second)}"
                else "${timeSlots.getOrNull(range.first) ?: ""}\n⋮"
                row.addView(TextView(context).apply {
                    text = timeRangeText; gravity = Gravity.CENTER; textSize = 10f
                    setBackgroundColor(Color.WHITE); setPadding(8, 8, 8, 8); minLines = 2
                    layoutParams = TableRow.LayoutParams(timeColumnWidth, actualRowHeight)
                })
                for (day in 1 until weekDays.size) {
                    val containerView = LinearLayout(context).apply {
                        layoutParams = TableRow.LayoutParams(0, actualRowHeight, 1f)
                        gravity = Gravity.CENTER; setBackgroundResource(R.drawable.timetable_cell_border)
                        orientation = LinearLayout.VERTICAL
                    }
                    classes.find { it.timeSlot == range.first && it.dayOfWeek == day && it.className.contains("비는 시간") }?.let { classItem ->
                        containerView.setBackgroundColor(classItem.color)
                        val classNames = classes.filter { it.dayOfWeek == day && it.timeSlot >= range.first && it.timeSlot <= range.second }
                            .map { it.className }.distinct().joinToString(", ")
                        containerView.setOnLongClickListener {
                            true
                        }
                        containerView.setOnClickListener { binding.timetableLayout.performClick() }
                        containerView.addView(View(context).apply {
                            layoutParams = LinearLayout.LayoutParams(1.dpToPx(context), LinearLayout.LayoutParams.MATCH_PARENT).apply { gravity = Gravity.END }
                            setBackgroundColor(Color.parseColor("#BBBBBB")); alpha = 0.5f
                        })
                    }
                    row.addView(containerView)
                }
                tableLayout.addView(row)
            } else {
                for (hourInMiddleRange in range.first..range.second) {
                    val singleHourRow = TableRow(context).apply { layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT) }
                    singleHourRow.addView(TextView(context).apply {
                        text = timeSlots.getOrNull(hourInMiddleRange) ?: ""; gravity = Gravity.CENTER; textSize = 10f
                        setBackgroundColor(Color.WHITE); setPadding(8, 8, 8, 8)
                        layoutParams = TableRow.LayoutParams(timeColumnWidth, cellHeight)
                    })
                    for (day in 1 until weekDays.size) {
                        val containerView = LinearLayout(context).apply {
                            layoutParams = TableRow.LayoutParams(0, cellHeight, 1f)
                            gravity = Gravity.CENTER; setBackgroundResource(R.drawable.timetable_cell_border)
                            orientation = LinearLayout.VERTICAL
                        }
                        classes.find { it.timeSlot == hourInMiddleRange && it.dayOfWeek == day && it.className.contains("비는 시간") }?.let { classItem ->
                            containerView.setBackgroundColor(classItem.color)
                            containerView.setOnLongClickListener {
                                true
                            }
                            containerView.setOnClickListener { binding.timetableLayout.performClick() }
                            containerView.addView(View(context).apply {
                                layoutParams = LinearLayout.LayoutParams(1.dpToPx(context), LinearLayout.LayoutParams.MATCH_PARENT).apply { gravity = Gravity.END }
                                setBackgroundColor(Color.parseColor("#BBBBBB")); alpha = 0.5f
                            })
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
        if (minTime > maxTime) return emptyList()
        val ranges = mutableListOf<Pair<Int, Int>>()
        val timeStatusMap = mutableMapOf<Int, MutableMap<Int, Int?>>()
        for (time in minTime..maxTime) {
            timeStatusMap[time] = mutableMapOf()
            for (day in 1..7) {
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
        ranges.add(Pair(rangeStart, maxTime))
        return ranges
    }

    private fun calculateTextWidth(text: String): Int {
        val currentContext = context ?: return 0
        val textView = TextView(currentContext).apply {
            this.text = text; setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        }
        return textView.measuredWidth
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class TimetableItem(
        val timeSlot: Int, val dayOfWeek: Int, val className: String, val color: Int
    )

    companion object {
        private const val ARG_MAITE_LIST_ITEM = "maite_list_item"
        fun newInstance(maiteListItem: MaiteListItem): ListDetailFragment {
            return ListDetailFragment().apply {
                arguments = Bundle().apply { putParcelable(ARG_MAITE_LIST_ITEM, maiteListItem) }
            }
        }
    }
}