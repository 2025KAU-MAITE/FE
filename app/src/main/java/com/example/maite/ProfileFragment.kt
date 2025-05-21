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
        
        Log.d("ProfileFragment", "User ID from preferences: $userId")
        Log.d("ProfileFragment", "Access Token: ${preferencesUtil.getAccessToken()?.take(10) ?: "NULL"}...")
        
        // 임시 테스트 용 사용자 ID 지정 (프로필 이미지 업로드 테스트용)
        val testUserId = 1L

        if (userId != null && userId != 0L) {
            // 서버에서 시간표 불러오기
            Log.d("ProfileFragment", "Loading timetable for userId: $userId")
            lifecycleScope.launch {
                viewModel.loadTimetableFromServer(userId)
            }
            // 사용자 정보 불러오기
            viewModel.loadUserInfo(userId)
        } else {
            // 테스트 사용자 ID 사용
            Log.e("ProfileFragment", "User ID not found or 0, using test ID: $testUserId")
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
                Log.d("ProfileFragment", "프로필 이미지 URL: ${it.profileImageUrl}")
                
                val preferencesUtil = PreferencesUtil(requireContext())
                val cachedImageUrl = preferencesUtil.getString("user_profile_image_url")
                
                if (!cachedImageUrl.isNullOrEmpty()) {
                    // 로컬에 저장된 URL이 있으면 우선 사용
                    Log.d("ProfileFragment", "로컬에 캐시된 이미지 URL 사용: $cachedImageUrl")
                    loadProfileImageFromUrl(cachedImageUrl)
                } else if (!it.profileImageUrl.isNullOrEmpty()) {
                    // 서버에서 받은 URL 사용
                    Log.d("ProfileFragment", "서버에서 받은 프로필 이미지 URL 사용: ${it.profileImageUrl}")
                    loadProfileImageFromUrl(it.profileImageUrl)
                } else {
                    // 둘 다 없으면 기본 이미지 사용
                    Log.d("ProfileFragment", "프로필 이미지 URL이 없어 기본 이미지 사용")
                    // Glide를 사용하여 기본 이미지도 원형으로 잘라냄
                    Glide.with(this@ProfileFragment)
                        .load(R.drawable.img_profile_default)
                        .circleCrop()
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

        // 친구 수 클릭 이벤트 추가 (친구 목록 프래그먼트로 이동)
        binding.tvMateCount.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_frm, MateListFragment())
                .addToBackStack(null)
                .commit()
        }

        // 상단 설정 버튼 클릭 이벤트 (추가 기능)
        binding.ivSettings.setOnClickListener {
            // TODO: 설정 화면으로 이동 또는 설정 메뉴 표시
        }
    }
    
    // URL을 사용해 프로필 이미지 로드
    private fun loadProfileImageFromUrl(url: String?) {
        if (url != null && url.isNotEmpty()) {
            Log.d("ProfileFragment", "URL을 사용해 프로필 이미지 로드: $url")
            
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
                    Log.d("ProfileFragment", "원격 URL을 로컬에 캐시 저장: $url")
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "원격 URL 로드 시 오류 발생: $url", e)
                    binding.ivProfile.setImageResource(R.drawable.img_profile_default)
                }
            } else {
                // URL이 로컬 URI일 수 있음
                try {
                    val uri = Uri.parse(url)
                    Log.d("ProfileFragment", "로컬 URI로 이미지 로드 시도: $url")
                    Glide.with(this@ProfileFragment)
                        .load(uri)
                        .circleCrop()
                        .skipMemoryCache(true) 
                        .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis().toString()))
                        .placeholder(R.drawable.img_profile_default)
                        .error(R.drawable.img_profile_default)
                        .into(binding.ivProfile)
                        
                    // 로컬 URI를 캡처해서 저장
                    val preferencesUtil = PreferencesUtil(requireContext())
                    preferencesUtil.setString("user_profile_image_uri", url)
                    Log.d("ProfileFragment", "로컬 URI를 저장 완료: $url")
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "${url}을 URI로 로드 시 오류 발생", e)
                    
                    // URI 파싱 실패 시 URL로 로드 시도
                    try {
                        Log.d("ProfileFragment", "URI 파싱 실패, 일반 URL로 로드 시도: $url")
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
                        Log.d("ProfileFragment", "URL 로드 성공, 로컬에 저장: $url")
                    } catch (e2: Exception) {
                        Log.e("ProfileFragment", "URL 로드도 실패하여 기본 이미지 사용", e2)
                        // Glide를 사용하여 기본 이미지도 원형으로 잘라냄
                        Glide.with(this)
                            .load(R.drawable.img_profile_default)
                            .circleCrop()
                            .into(binding.ivProfile)
                    }
                }
            }
        } else {
            Log.d("ProfileFragment", "유효하지 않은 URL. 기본 이미지 표시")
            // Glide를 사용하여 기본 이미지도 원형으로 잘라냄
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
            Log.d("ProfileFragment", "서버 URL 사용: $imageSource")
        } 
        // 2. 로컬 URI 확인
        else if (!preferencesUtil.getProfileImageUri().isNullOrEmpty()) {
            imageSource = preferencesUtil.getProfileImageUri()
            Log.d("ProfileFragment", "로컬 URI 사용: $imageSource")
        } 
        // 3. 캐시된 URL 확인
        else if (!preferencesUtil.getProfileImageUrl().isNullOrEmpty()) {
            imageSource = preferencesUtil.getProfileImageUrl()
            Log.d("ProfileFragment", "캐시된 URL 사용: $imageSource")
        }
        
        // 디버깅 정보 로그
        Log.d("ProfileFragment", "바텀시트 열기 전 정보 확인:")
        Log.d("ProfileFragment", "- 서버 URL: $userProfileImageUrl")
        Log.d("ProfileFragment", "- 로컬 URI: ${preferencesUtil.getProfileImageUri()}")
        Log.d("ProfileFragment", "- 캐시된 URL: ${preferencesUtil.getProfileImageUrl()}")
        Log.d("ProfileFragment", "- 전달할 이미지 소스: $imageSource")
        
        // 바텀시트 생성 및 이미지 소스 전달
        val bottomSheet = ProfileEditBottomSheet.newInstance(imageSource)
        bottomSheet.setProfileImageUpdateListener(this) // 리스너 설정 추가
        bottomSheet.show(childFragmentManager, ProfileEditBottomSheet.TAG)
    }

    // 프로필 이미지 업데이트 콜백
    override fun onProfileImageUpdated() {
        try {
            Log.d("ProfileFragment", "onProfileImageUpdated 호출 - 이미지 업데이트 시도")
            val preferencesUtil = PreferencesUtil(requireContext())
            
            // 1. Glide 캐시 완전히 초기화 (매우 중요 - 이미지 수정 후 반드시 새로고침되도록)
            try {
                Log.d("ProfileFragment", "Glide 캐시 완전 초기화 시작")
                // 메인 스레드에서 메모리 캐시 초기화
                Glide.get(requireContext()).clearMemory()
                
                // 백그라운드 스레드에서 디스크 캐시 초기화 
                Thread {
                    try {
                        Glide.get(requireContext()).clearDiskCache()
                        Log.d("ProfileFragment", "Glide 디스크 캐시 초기화 완료")
                    } catch (e: Exception) {
                        Log.e("ProfileFragment", "Glide 디스크 캐시 초기화 실패", e)
                    }
                }.start()
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Glide 캐시 초기화 중 오류", e)
            }
            
            // 2. 임시 URI 확인 및 처리 (바텀시트에서 임시 저장한 값)
            val tempImageUri = preferencesUtil.getString("user_profile_image_uri_temp")
            if (!tempImageUri.isNullOrEmpty()) {
                Log.d("ProfileFragment", "임시 URI 발견: $tempImageUri - 정식 URI로 저장")
                // 임시 URI를 정식 URI로 복사 (PreferencesUtil 메소드 사용)
                preferencesUtil.saveProfileImageUri(tempImageUri)
                preferencesUtil.removeString("user_profile_image_uri_temp")
            }
            
            // 3. 현재 URI 또는 URL 상태 확인
            val currentUri = preferencesUtil.getProfileImageUri()
            val currentUrl = preferencesUtil.getProfileImageUrl()
            
            Log.d("ProfileFragment", "현재 상태 - URI: $currentUri, URL: $currentUrl")
            
            // 4. URI로 이미지 로드 시도
            if (!currentUri.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(currentUri)
                    Log.d("ProfileFragment", "URI로 이미지 로드 시도: $currentUri")
                    
                    // 캐시 사용 안 함 + 서명 추가로 강제 새로고침
                    Glide.with(this)
                        .load(uri)
                        .circleCrop()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                        .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis()))
                        .placeholder(R.drawable.img_profile_default)
                        .error(R.drawable.img_profile_default)
                        .into(binding.ivProfile)
                    
                    Log.d("ProfileFragment", "URI로 이미지 로드 성공")
                    return
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "URI로 이미지 로드 실패: $currentUri", e)
                }
            }
            
            // 5. URL로 이미지 로드 시도
            if (!currentUrl.isNullOrEmpty()) {
                try {
                    Log.d("ProfileFragment", "URL로 이미지 로드 시도: $currentUrl")
                    
                    // 캐시 사용 안 함 + 서명 추가로 강제 새로고침
                    Glide.with(this)
                        .load(currentUrl)
                        .circleCrop()
                        .skipMemoryCache(true)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                        .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis()))
                        .placeholder(R.drawable.img_profile_default)
                        .error(R.drawable.img_profile_default)
                        .into(binding.ivProfile)
                        
                    Log.d("ProfileFragment", "URL로 이미지 로드 성공")
                    return
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "URL로 이미지 로드 실패: $currentUrl", e)
                }
            }
            
            // 6. 모든 시도 실패 시 서버에서 정보 새로고침
            Log.d("ProfileFragment", "로컬 이미지 로드 실패, 서버에서 정보 새로고침 시도")
            
            val userId = preferencesUtil.getUserId()
            if (userId != null && userId != 0L) {
                Log.d("ProfileFragment", "서버에서 사용자 정보 로드: userId=$userId")
                viewModel.loadUserInfo(userId)
            } else {
                // 테스트 사용자 ID로 정보 불러오기
                val testUserId = 1L
                Log.d("ProfileFragment", "서버에서 테스트 사용자 정보 로드: userId=$testUserId")
                viewModel.loadUserInfo(testUserId)
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "프로필 이미지 업데이트 중 예외 발생", e)
        }
    }

    override fun onResume() {
        super.onResume()

        Log.d("ProfileFragment", "onResume 호출 - 프로필 화면 다시 표시")
        
        // Glide 캐시 초기화 (중요 - 프로필 이미지 재로드 위해)
        try {
            // 메모리 캐시는 메인 스레드에서 처리
            Glide.get(requireContext()).clearMemory()
            
            // 디스크 캐시는 백그라운드 스레드에서 처리
            Thread {
                try {
                    Glide.get(requireContext()).clearDiskCache()
                    Log.d("ProfileFragment", "onResume: Glide 디스크 캐시 지우기 성공")
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "onResume: Glide 디스크 캐시 지우기 오류", e)
                }
            }.start()
        } catch (e: Exception) {
            Log.e("ProfileFragment", "onResume: Glide 캐시 지우기 오류", e)
        }
        
        // 프로필 이미지 정보 가져오기
        val preferencesUtil = PreferencesUtil(requireContext())
        val userId = preferencesUtil.getUserId()
        
        // 먼저 로컬 URI 확인 (최우선)
        val localImageUri = preferencesUtil.getProfileImageUri()
        if (!localImageUri.isNullOrEmpty()) {
            Log.d("ProfileFragment", "onResume: 로컬 URI 사용 이미지 로드: $localImageUri")
            try {
                val uri = Uri.parse(localImageUri)
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                    .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis()))
                    .placeholder(R.drawable.img_profile_default)
                    .error(R.drawable.img_profile_default)
                    .into(binding.ivProfile)
                
                Log.d("ProfileFragment", "onResume: 로컬 URI로 이미지 로드 성공")
            } catch (e: Exception) {
                Log.e("ProfileFragment", "onResume: 로컬 URI 로드 오류", e)
                // 로딩 실패 시 URL 사용 시도
            }
        } 
        
        // 로컬 URI가 없거나 로드 실패한 경우 URL 사용
        val cachedImageUrl = preferencesUtil.getProfileImageUrl()
        if (!cachedImageUrl.isNullOrEmpty()) {
            Log.d("ProfileFragment", "onResume: 캐시된 URL 이미지 로드: $cachedImageUrl")
            loadProfileImageFromUrl(cachedImageUrl)
        }
        
        // 서버에서 사용자 정보 업데이트
        if (userId != null && userId != 0L) {
            Log.d("ProfileFragment", "onResume: 사용자 정보 로딩 userId: $userId")
            // 사용자 정보 로딩 (프로필 이미지 URL 포함)
            viewModel.loadUserInfo(userId)
            
            // 시간표 로딩
            lifecycleScope.launch {
                viewModel.loadTimetableFromServer(userId)
            }
        } else {
            // 테스트 사용자 ID를 사용
            val testUserId = 1L
            Log.d("ProfileFragment", "onResume: 테스트 사용자 정보 로딩 userId: $testUserId")
            viewModel.loadUserInfo(testUserId)
            
            lifecycleScope.launch {
                viewModel.loadTimetableFromServer(testUserId)
            }
        }
    }

    // 개선된 시간표 UI 생성 - 30분 단위 정확한 표시를 위한 수정
    private fun createTimetable(entries: List<TimetableEntry>) {
        Log.d("ProfileFragment", "createTimetable called with ${entries.size} entries")
        
        val tableLayout = binding.timetableLayout
        tableLayout.removeAllViews()

        // 동적 시간 범위 계산 - 24시까지 표시 가능하도록 수정
        var minHour = 9 // 기본 최소 시간 (9시)
        var maxHour = 24 // 기본 최대 시간을 24시로 설정 (21시 이후 잘림 문제 해결)

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

        // 시간 범위가 넘어가면 제한 (0-24 범위 내로)
        minHour = minHour.coerceIn(0, 23)
        maxHour = maxHour.coerceIn(minHour + 1, 24) // 최대 24시까지 허용

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

        // 1시간 단위 셀 높이 설정 (30분 단위 정확한 위치는 내부적으로 계산)
        val hourCellHeight = resources.getDimensionPixelSize(R.dimen.timetable_cell_height)

        // 시간 행 추가 (1시간 단위로 UI 표시)
        for (hour in minHour until maxHour) {
            val row = TableRow(context)

            // 시간 셀
            val timeCell = createTextView(hour.toString(), timeColWidth, hourCellHeight)
            timeCell.setBackgroundColor(Color.parseColor("#F5F5F5"))
            timeCell.textSize = 10f
            timeCell.setTextColor(Color.parseColor("#555555"))
            row.addView(timeCell)

            // 요일별 셀 추가 (1시간 단위)
            for (day in 1 until weekDays.size) {
                // 해당 시간에 해당하는 일정 찾기
                val entriesInThisHour = entries.filter { e ->
                    // 일정 시작 및 종료 시간 (분 단위)
                    val startTime = e.startHour * 60 + e.startMinute
                    val endTime = e.endHour * 60 + e.endMinute
                    
                    // 현재 시간 범위 (분 단위)
                    val hourStart = hour * 60
                    val hourEnd = (hour + 1) * 60
                    
                    // 해당 요일에 일정이 현재 시간 범위와 겹치는지 확인
                    e.dayOfWeek == day && 
                            !(endTime <= hourStart || startTime >= hourEnd)
                }

                if (entriesInThisHour.isEmpty()) {
                    // 빈 셀 추가
                    val emptyCell = LinearLayout(context).apply {
                        layoutParams = TableRow.LayoutParams(0, hourCellHeight, 1f)
                        setBackgroundResource(R.drawable.timetable_cell_border)
                    }
                    row.addView(emptyCell)
                } else {
                    // 현재 시간대의 일정 찾기 (여러 개 일정이 겹치는 경우 첫 번째 사용)
                    val entry = entriesInThisHour[0]
                    
                    // 현재 시간대에서 일정의 시작과 끝 위치 계산
                    val startTimeInMinutes = entry.startHour * 60 + entry.startMinute
                    val endTimeInMinutes = entry.endHour * 60 + entry.endMinute
                    
                    // 현재 시간대의 시작과 끝
                    val hourStartMinutes = hour * 60
                    val hourEndMinutes = (hour + 1) * 60
                    
                    // 일정의 상대적 위치 확인
                    val isStartHour = startTimeInMinutes >= hourStartMinutes && startTimeInMinutes < hourEndMinutes
                    val isEndHour = endTimeInMinutes > hourStartMinutes && endTimeInMinutes <= hourEndMinutes
                    
                    // 셀 생성 - 30분 단위 정확한 표시를 위한 개선
                    val cell = LinearLayout(context).apply {
                        // 30분 단위 정확한 위치를 위한 파라미터 계산
                        val startMinutes = entry.startHour * 60 + entry.startMinute
                        val endMinutes = entry.endHour * 60 + entry.endMinute
                        val hourStartMinutes = hour * 60
                        val hourEndMinutes = (hour + 1) * 60
                        
                        // 현재 시간대 내에서의 상대적 시작 위치 (0~1 사이 비율)
                        val startRatio = if (startMinutes <= hourStartMinutes) 0f
                                         else (startMinutes - hourStartMinutes) / 60f
                        
                        // 현재 시간대 내에서의 상대적 끝 위치 (0~1 사이 비율)
                        val endRatio = if (endMinutes >= hourEndMinutes) 1f
                                       else (endMinutes - hourStartMinutes) / 60f
                        
                        // 시작 위치에 따른 상단 마진 계산
                        val topMargin = (hourCellHeight * startRatio).toInt()
                        
                        // 시간 비율에 따른 높이 계산
                        val heightRatio = endRatio - startRatio
                        val cellContentHeight = (hourCellHeight * heightRatio).toInt()
                        
                        // 레이아웃 파라미터 설정 (상대적 위치로)
                        layoutParams = TableRow.LayoutParams(0, cellContentHeight, 1f).apply {
                            this.topMargin = topMargin
                        }
                        
                        gravity = Gravity.CENTER
                        orientation = LinearLayout.VERTICAL
                        
                        // 배경색 설정
                        setBackgroundColor(Color.parseColor(entry.colorHex))
                        alpha = 0.85f
                        
                        // 텍스트 표시 - 시작 시간에만 제목 표시, 끝 시간에 장소 표시
                        if (isStartHour) {
                            // 시작 시간을 포함하는 정확한 시간 표시
                            addView(TextView(context).apply {
                                // 정확한 시간 표시 (30분 단위 포함)
                                val timeText = String.format("%02d:%02d", entry.startHour, entry.startMinute)
                                text = "${entry.title} $timeText"
                                textSize = 10f
                                gravity = Gravity.CENTER
                                setTextColor(Color.WHITE)
                                ellipsize = android.text.TextUtils.TruncateAt.END
                                maxLines = 1
                                setPadding(2, 2, 2, 2)
                            })
                        } else if (isEndHour && !entry.location.isNullOrEmpty() && 
                                   (endMinutes - startMinutes) >= 60) {
                            // 종료 시간에는 장소 표시 (일정이 1시간 이상인 경우만)
                            addView(TextView(context).apply {
                                // 정확한 종료 시간도 표시
                                val endTimeText = String.format("%02d:%02d", entry.endHour, entry.endMinute)
                                text = "장소:${entry.location} (~$endTimeText)"
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