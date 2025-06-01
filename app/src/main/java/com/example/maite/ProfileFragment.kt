package com.example.maite

import android.graphics.Color
import android.net.Uri
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
        
        // 임시 테스트 용 사용자 ID 지정 (프로필 이미지 업로드 테스트용)
        val testUserId = 1L

        // 설정 버튼 클릭 이벤트 설정
        binding.ivSettings.setOnClickListener {
            navigateToSettings()
        }

        if (userId != null && userId != 0L) {
            // 서버에서 시간표 불러오기
            lifecycleScope.launch {
                viewModel.loadTimetableFromServer(userId)
            }
            // 사용자 정보 불러오기
            viewModel.loadUserInfo(userId)
        } else {
            // 테스트 사용자 ID 사용
            lifecycleScope.launch {
                viewModel.loadTimetableFromServer(testUserId)
            }
            viewModel.loadUserInfo(testUserId)
        }

        // 사용자 정보 관찰
        viewModel.userInfo.observe(viewLifecycleOwner) { userInfo: UserInfo? ->
            userInfo?.let {
                binding.tvName.text = it.name
                // 친구 수 표시 (인스타그램 스타일)
                // LinearLayout 내의 첫 번째 TextView에 수치 설정
                ((binding.tvMateCount as LinearLayout).getChildAt(0) as TextView).text = it.mateCount.toString()

                // 프로필 이미지 로드 - 로컬 캐시 우선 사용
                val preferencesUtil = PreferencesUtil(requireContext())
                val cachedImageUrl = preferencesUtil.getString("user_profile_image_url")
                
                if (!cachedImageUrl.isNullOrEmpty()) {
                    // 로컬에 저장된 URL이 있으면 우선 사용
                    loadProfileImageFromUrl(cachedImageUrl)
                } else if (!it.profileImageUrl.isNullOrEmpty()) {
                    // 서버에서 받은 URL 사용
                    loadProfileImageFromUrl(it.profileImageUrl)
                } else {
                    // 둘 다 없으면 기본 이미지 사용
                    Glide.with(this@ProfileFragment)
                        .load(R.drawable.img_profile_default)
                        .circleCrop()
                        .into(binding.ivProfile)
                }
            }
        }

        // 시간표 데이터 관찰
        viewModel.timetable.observe(viewLifecycleOwner) { timetableList ->
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

        // 친구 수 클릭 이벤트 추가 (친구 목록 프래그먼트로 이동)
        binding.tvMateCount.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, MateListFragment())
                .addToBackStack(null)
                .commit()
        }

        // 상단 설정 버튼 클릭 이벤트 (추가 기능)
        binding.ivSettings.setOnClickListener {
            navigateToSettings()
        }
    }
    
    // URL을 사용해 프로필 이미지 로드
    private fun loadProfileImageFromUrl(url: String?) {
        if (url != null && url.isNotEmpty()) {
            // 실제 URL로 시작하는지 확인
            val isRemoteUrl = url.startsWith("http") || url.startsWith("https")
            
            if (isRemoteUrl) {
                // 원격 URL 사용
                try {
                    Glide.with(this@ProfileFragment)
                        .load(url)
                        .circleCrop()
                        .skipMemoryCache(true) // 메모리 캐시 사용 안 함
                        .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis().toString())) // 항상 새로 로드
                        .placeholder(R.drawable.img_profile_default)
                        .error(R.drawable.img_profile_default)
                        .into(binding.ivProfile)
                    
                    // 성공적으로 로드된 URL을 로컬에 캐시 저장
                    val preferencesUtil = PreferencesUtil(requireContext())
                    preferencesUtil.setString("user_profile_image_url", url)
                } catch (e: Exception) {
                    binding.ivProfile.setImageResource(R.drawable.img_profile_default)
                }
            } else {
                // URL이 로컬 URI일 수 있음
                try {
                    val uri = Uri.parse(url)
                    Glide.with(this@ProfileFragment)
                        .load(uri)
                        .circleCrop()
                        .skipMemoryCache(true) 
                        .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis().toString()))
                        .placeholder(R.drawable.img_profile_default)
                        .error(R.drawable.img_profile_default)
                        .into(binding.ivProfile)
                        
                    // 로컬 URI를 캐시에 저장
                    val preferencesUtil = PreferencesUtil(requireContext())
                    preferencesUtil.setString("user_profile_image_uri", url)
                } catch (e: Exception) {
                    // URI 파싱 실패 시 URL로 로드 시도
                    try {
                        Glide.with(this@ProfileFragment)
                            .load(url)
                            .circleCrop()
                            .skipMemoryCache(true)
                            .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis().toString()))
                            .placeholder(R.drawable.img_profile_default)
                            .error(R.drawable.img_profile_default)
                            .into(binding.ivProfile)
                            
                        // 성공한 URL 저장
                        val preferencesUtil = PreferencesUtil(requireContext())
                        preferencesUtil.setString("user_profile_image_url", url)
                    } catch (e2: Exception) {
                        // 모든 로드 방법 실패 시 기본 이미지 사용
                        Glide.with(this)
                            .load(R.drawable.img_profile_default)
                            .circleCrop()
                            .into(binding.ivProfile)
                    }
                }
            }
        } else {
            // 유효하지 않은 URL일 때 기본 이미지 사용
            Glide.with(this)
                .load(R.drawable.img_profile_default)
                .circleCrop()
                .into(binding.ivProfile)
        }
    }

    // 프로필 수정 바텀시트 표시
    private fun showProfileEditBottomSheet() {
        // 사용자 정보에서 프로필 이미지 URL 가져오기
        val userProfileImageUrl = viewModel.userInfo.value?.profileImageUrl
        
        // 현재 이미지 소스 확인 (우선순위: 서버 URL > 로컬 URI > 캐시된 URL)
        val preferencesUtil = PreferencesUtil(requireContext())
        var imageSource: String? = null
        
        // 1. 서버에서 가져온 이미지 URL이 있으면 우선 사용
        if (!userProfileImageUrl.isNullOrEmpty()) {
            imageSource = userProfileImageUrl
        } 
        // 2. 로컬 URI 확인
        else if (!preferencesUtil.getProfileImageUri().isNullOrEmpty()) {
            imageSource = preferencesUtil.getProfileImageUri()
        } 
        // 3. 캐시된 URL 확인
        else if (!preferencesUtil.getProfileImageUrl().isNullOrEmpty()) {
            imageSource = preferencesUtil.getProfileImageUrl()
        }
        
        // 바텀시트 생성 및 이미지 소스 전달
        val bottomSheet = ProfileEditBottomSheet.newInstance(imageSource)
        bottomSheet.setProfileImageUpdateListener(this) // 리스너 설정 추가
        bottomSheet.show(childFragmentManager, ProfileEditBottomSheet.TAG)
    }
    
    // 설정 화면으로 이동
    private fun navigateToSettings() {
        val settingsFragment = com.example.maite.ui.settings.SettingsFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_frm, settingsFragment)
            .addToBackStack(null)
            .commit()
    }

    // 프로필 이미지 업데이트 콜백 (성능 최적화)
    override fun onProfileImageUpdated() {
    try {
    val preferencesUtil = PreferencesUtil(requireContext())
    
    // 임시 URI 확인 및 처리
    val tempImageUri = preferencesUtil.getString("user_profile_image_uri_temp")
    if (!tempImageUri.isNullOrEmpty()) {
    preferencesUtil.saveProfileImageUri(tempImageUri)
    preferencesUtil.removeString("user_profile_image_uri_temp")
    }
    
    // 현재 URI 또는 URL로 이미지 로드
    val currentUri = preferencesUtil.getProfileImageUri()
    val currentUrl = preferencesUtil.getProfileImageUrl()
    
    if (!currentUri.isNullOrEmpty()) {
    try {
            val uri = Uri.parse(currentUri)
        Glide.with(this)
                .load(uri)
                .circleCrop()
                .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis()))
                .placeholder(R.drawable.img_profile_default)
                .error(R.drawable.img_profile_default)
            .into(binding.ivProfile)
        return
    } catch (e: Exception) {
            // URI 로드 실패 시 URL 시도
        }
    }
    
    if (!currentUrl.isNullOrEmpty()) {
        loadProfileImageFromUrl(currentUrl)
    }
    } catch (e: Exception) {
    // 프로필 이미지 업데이트 실패 시 무시
    }
    }

    override fun onResume() {
        super.onResume()
        
        // 성능 최적화: 프로필 이미지만 간단하게 새로고침
        val preferencesUtil = PreferencesUtil(requireContext())
        
        // 로컬 URI가 있으면 우선 사용
        val localImageUri = preferencesUtil.getProfileImageUri()
        if (!localImageUri.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(localImageUri)
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .placeholder(R.drawable.img_profile_default)
                    .error(R.drawable.img_profile_default)
                    .into(binding.ivProfile)
            } catch (e: Exception) {
                // 로딩 실패 시 캐시된 URL 사용
                val cachedImageUrl = preferencesUtil.getProfileImageUrl()
                if (!cachedImageUrl.isNullOrEmpty()) {
                    loadProfileImageFromUrl(cachedImageUrl)
                }
            }
        }
    }

    // 개선된 시간표 UI 생성 - 프로필 화면에 맞게 제한된 크기로 수정
    private fun createTimetable(entries: List<TimetableEntry>) {
        try {
            val tableLayout = binding.timetableLayout
            tableLayout.removeAllViews()

            // 동적 시간 범위 계산 - 23시까지 표시
            var minHour = 8 // 기본 최소 시간
            var maxHour = 24 // 23시까지 표시 (until 사용으로 23시까지)

            // 일정이 있는 경우에만 범위 조정
            if (entries.isNotEmpty()) {
                val startTimes = entries.map { it.startHour + (it.startMinute / 60.0) }
                val endTimes = entries.map { it.endHour + (it.endMinute / 60.0) }

                startTimes.minOrNull()?.let { minStart ->
                    if (minStart.toInt() < minHour) {
                        minHour = maxOf(minStart.toInt(), 8) // 8시 이후로 제한
                    }
                }

                endTimes.maxOrNull()?.let { maxEnd ->
                    if (ceil(maxEnd).toInt() > maxHour) {
                        maxHour = minOf(ceil(maxEnd).toInt(), 24) // 24시까지 허용 (23시 셀 표시)
                    }
                }
            }

            // 시간 범위 제한
            minHour = minHour.coerceIn(8, 22)
            maxHour = maxHour.coerceIn(minHour + 1, 24) // 24시까지 허용 (23시 셀 표시)

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

            // 시간 행 추가 - 안전한 버전
            val hourCellHeight = try {
                resources.getDimensionPixelSize(R.dimen.timetable_cell_height)
            } catch (e: Exception) {
                100 // 기본값 사용
            }

            for (hour in minHour until maxHour) {
                val row = TableRow(context)

                // 시간 셀
                val timeCell = createTextView(hour.toString(), timeColWidth, hourCellHeight)
                timeCell.setBackgroundColor(Color.parseColor("#F5F5F5"))
                timeCell.textSize = 10f
                timeCell.setTextColor(Color.parseColor("#555555"))
                row.addView(timeCell)

                // 요일별 셀 추가
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
                        val emptyCell = LinearLayout(context).apply {
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
                        
                        val cell = LinearLayout(context).apply {
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
                                addView(TextView(context).apply {
                                    text = entry.title
                                    textSize = 10f
                                    gravity = Gravity.CENTER
                                    setTextColor(Color.WHITE)
                                    ellipsize = android.text.TextUtils.TruncateAt.END
                                    maxLines = 1
                                    setPadding(2, 2, 2, 2)
                                })
                            } else if (isEndHour && !entry.location.isNullOrEmpty() && 
                                       (endTimeInMinutes - startTimeInMinutes) >= 60) {
                                addView(TextView(context).apply {
                                    text = "장소:${entry.location}"
                                    textSize = 8f
                                    gravity = Gravity.CENTER
                                    setTextColor(Color.WHITE)
                                    ellipsize = android.text.TextUtils.TruncateAt.END
                                    maxLines = 1
                                    setPadding(2, 0, 2, 0)
                                })
                            }
                        }
                        
                        row.addView(cell)
                    }
                }

                tableLayout.addView(row)
            }
        } catch (e: Exception) {
            // 에러 발생 시 기본 메시지 표시
            val errorTextView = TextView(context).apply {
                text = "시간표를 불러오는 중 문제가 발생했습니다."
                gravity = Gravity.CENTER
                setPadding(16, 16, 16, 16)
            }
            binding.timetableLayout.removeAllViews()
            binding.timetableLayout.addView(errorTextView)
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
