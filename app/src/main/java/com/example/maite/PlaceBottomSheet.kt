package com.example.maite

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.maite.databinding.BottomSheetPlaceBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaceBottomSheet : BottomSheetDialogFragment(), OnMapReadyCallback {
    private var _binding: BottomSheetPlaceBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private var naverMap: NaverMap? = null
    private var selectedLatLng: LatLng? = null // 선택된 위치 좌표 저장
    private var currentMarker: Marker? = null // 선택된 위치에 표시할 마커
    private var selectedPlaceName: String? = null // 선택된 장소 이름 저장
    private lateinit var apiService: MaiteApiService
    private var cafeMarkers: MutableList<Marker> = mutableListOf() // 카페 마커 목록

    // AI 버튼 표시 여부를 제어하는 플래그 추가
    private var showAiButton: Boolean = false

    // 회의 ID 저장 변수
    private var meetingId: Long = -1

    // --- 리스너 인터페이스 (이름도 전달) ---
    interface OnPlaceSelectedListener {
        fun onPlaceSelected(latLng: LatLng, name: String?)
    }
    private var placeSelectedListener: OnPlaceSelectedListener? = null

    fun setOnPlaceSelectedListener(listener: OnPlaceSelectedListener) {
        this.placeSelectedListener = listener
    }
    // --- 리스너 끝 ---

    // --- 완료 버튼 상태 업데이트 함수 ---
    private fun updateDoneButtonState(isEnabled: Boolean) {
        // 바인딩 유효성 체크
        if (_binding == null) {
            Log.w("PlaceBottomSheet", "updateDoneButtonState 호출 시 바인딩이 null입니다.")
            return
        }

        binding.doneBtn.isEnabled = isEnabled
        binding.doneBtn.isClickable = isEnabled // 비활성화 시 클릭도 막음

        // Context 가져오기 (null 체크 포함)
        val context = context ?: run {
            Log.e("PlaceBottomSheet", "Context가 null이라 버튼 색상 업데이트 불가.")
            return
        }

        // colors.xml에 정의된 색상 리소스 사용 (TimePicker와 동일하다고 가정)
        // 실제 사용하는 색상 리소스 이름으로 변경해야 할 수 있습니다. (예: R.color.purple_500, R.color.grey)
        val bgColorRes = if (isEnabled) R.color.mainColor else R.color.btn_inactive // 활성/비활성 배경 색상
        val textColorRes = if (isEnabled) R.color.white else R.color.black // 활성/비활성 텍스트 색상

        try {
            val bgColor = ContextCompat.getColor(context, bgColorRes)
            val textColor = ContextCompat.getColor(context, textColorRes)

            // ImageView 배경에 색상 필터 적용, TextView 텍스트 색상 설정
            binding.btnBg.setColorFilter(bgColor)
            binding.btnText.setTextColor(textColor)

            Log.d("PlaceBottomSheet", "완료 버튼 상태 업데이트됨. 활성화: $isEnabled")
        } catch (e: Exception) {
            Log.e("PlaceBottomSheet", "버튼 색상 리소스 로드 중 오류 발생", e)
            // 리소스 로드 실패 시 기본 상태 유지 또는 다른 처리
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPlaceBinding.inflate(inflater, container, false)
        mapView = binding.mapView

        // API 서비스 초기화
        apiService = MaiteRetrofitClient.getInstance(requireContext())

        // Bundle에서 설정값 가져오기
        showAiButton = arguments?.getBoolean(ARG_SHOW_AI_BUTTON, false) ?: false
        meetingId = arguments?.getLong(ARG_MEETING_ID, -1) ?: -1

        // *** MapView 생명주기 이벤트 전파 ***
        mapView.onCreate(savedInstanceState)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // AI 버튼/툴팁 가시성 설정
        binding.aiCardView.visibility = if (showAiButton) View.VISIBLE else View.GONE
        binding.aiButtonTooltip2.visibility = if (showAiButton) View.GONE else View.GONE // 기본은 GONE, 표시할 때만 VISIBLE로 설정

        // AI 버튼이 표시되는 경우에만 툴팁 표시 로직 추가
        if (showAiButton) {
            // 일정 시간 후 툴팁 표시 (예: 1초 후)
            view.postDelayed({
                if (isAdded && _binding != null) { // Fragment가 아직 활성 상태인지 확인
                    binding.aiButtonTooltip2.visibility = View.VISIBLE
                }
            }, 1000)

            // 툴팁 자동 숨김 (예: 4초 후)
            view.postDelayed({
                if (isAdded && _binding != null) {
                    binding.aiButtonTooltip2.visibility = View.GONE
                }
            }, 4000)

            // AI 버튼 클릭 이벤트 설정
            binding.btnAi.setOnClickListener {
                Log.d("PlaceBottomSheet", "AI 버튼 클릭됨")

                if (meetingId > 0) {
                    fetchNearbyCafes(meetingId)
                } else {
                    Toast.makeText(context, "유효한 회의 ID가 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 비동기적으로 지도 로딩 시작
        mapView.getMapAsync(this)

        binding.doneBtn.setOnClickListener {
            // 버튼 비활성화 시 클릭 무시 (추가)
            if (!binding.doneBtn.isEnabled) {
                Log.w("PlaceBottomSheet", "완료 버튼 비활성화 상태 클릭됨.")
                // Toast.makeText(context, "먼저 장소를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 선택된 위치와 이름 함께 전달 (기존 로직)
            selectedLatLng?.let { latLng ->
                placeSelectedListener?.onPlaceSelected(latLng, selectedPlaceName)
                Log.d("PlaceBottomSheet", "선택 완료: ${selectedPlaceName ?: "이름 없음"} (Lat=${latLng.latitude}, Lng=${latLng.longitude})")
            }
            dismiss() // 바텀 시트 닫기
        }
        binding.searchBtn.setOnClickListener {
            // TODO: 장소 검색 로직 구현 (예: 검색 화면으로 이동)
        }

        // --- 바텀 시트 확장 코드 ---
        view.viewTreeObserver.addOnGlobalLayoutListener {
            val dialog = dialog as? BottomSheetDialog ?: return@addOnGlobalLayoutListener
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? FrameLayout ?: return@addOnGlobalLayoutListener
            val behavior = BottomSheetBehavior.from(bottomSheet)

            behavior.isDraggable = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }

        // 초기 버튼 상태 설정은 onMapReady에서 수행
    }

    // API를 호출하여 주변 카페 정보를 가져오는 함수
    private fun fetchNearbyCafes(meetingId: Long) {

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getNearbyCafes(meetingId)
                }

                if (response.isSuccessful) {
                    val cafes = response.body() ?: emptyList()
                    Log.d("PlaceBottomSheet", "주변 카페 ${cafes.size}개 조회됨")

                    if (cafes.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "주변 카페가 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            // 기존 카페 마커 제거
                            clearCafeMarkers()

                            // 지도에 카페 표시
                            displayCafesOnMap(cafes)

                            // 카페 목록 중앙으로 카메라 이동 (첫 번째 카페 위치로 이동)
                            moveCameraToFirstCafe(cafes)
                        }
                    }
                } else {
                    Log.e("PlaceBottomSheet", "API 오류: ${response.code()} ${response.message()}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "카페 검색 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("PlaceBottomSheet", "API 호출 실패", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 기존 카페 마커 제거
    private fun clearCafeMarkers() {
        for (marker in cafeMarkers) {
            marker.map = null
        }
        cafeMarkers.clear()
    }

    // 지도에 카페 표시
    private fun displayCafesOnMap(cafes: List<NearByCafe>) {
        val cafeMarkerIcon = OverlayImage.fromResource(R.drawable.ic_marker) // 카페용 아이콘 (준비 필요)

        naverMap?.let { map ->
            for (cafe in cafes) {
                try {
                    // String을 Double로 변환
                    val lat = cafe.mapy.toDoubleOrNull()?.let { it / 10000000.0 } ?: continue
                    val lng = cafe.mapx.toDoubleOrNull()?.let { it / 10000000.0 } ?: continue

                    val cafePosition = LatLng(lat, lng)
                    val marker = Marker().apply {
                        position = cafePosition
                        icon = cafeMarkerIcon
                        width = 100  // 카페 마커 크기
                        height = 100
                        captionText = cafe.title
                        tag = cafe  // 마커에 카페 정보 저장

                        context?.let { ctx ->
                            iconTintColor = ContextCompat.getColor(ctx, R.color.mainColor) // 카페 마커 색상
                        }

                        this.map = map
                    }

                    // 마커 클릭 이벤트 설정
                    marker.setOnClickListener {
                        // 선택된 카페로 설정
                        selectedLatLng = cafePosition
                        selectedPlaceName = cafe.title + " (" + cafe.roadAddress + ")"

                        // 검색창 텍스트 업데이트
                        binding.textView20.text = selectedPlaceName

                        // 완료 버튼 활성화
                        updateDoneButtonState(true)

                        // 이벤트 소비
                        true
                    }

                    cafeMarkers.add(marker)
                } catch (e: Exception) {
                    Log.e("PlaceBottomSheet", "카페 마커 생성 오류", e)
                }
            }
        }
    }

    // 첫 번째 카페로 카메라 이동
    private fun moveCameraToFirstCafe(cafes: List<NearByCafe>) {
        if (cafes.isNotEmpty() && naverMap != null) {
            try {
                val lat = cafes[0].mapy.toDoubleOrNull()?.let { it / 10000000.0 }
                val lng = cafes[0].mapx.toDoubleOrNull()?.let { it / 10000000.0 }

                if (lat != null && lng != null) {
                    val cameraUpdate = CameraUpdate.scrollTo(LatLng(lat, lng))
                    naverMap?.moveCamera(cameraUpdate)
                }
            } catch (e: Exception) {
                Log.e("PlaceBottomSheet", "카메라 이동 오류", e)
            }
        }
    }

    override fun onMapReady(naverMap: NaverMap) {
        this.naverMap = naverMap
        Log.d("PlaceBottomSheet", "네이버 지도 준비 완료.")

        // --- 초기 카메라 위치 설정 (예: 서울 시청) ---
        val initialPosition = LatLng(37.5666102, 126.9783881)
        val cameraUpdate = CameraUpdate.scrollTo(initialPosition)
        naverMap.moveCamera(cameraUpdate)
        // --- 초기 위치 끝 ---

        // --- 초기 버튼 상태 설정 (여기서 호출) ---
        updateDoneButtonState(false) // 지도 준비 후, 초기에는 비활성화

        val customMarkerIcon = OverlayImage.fromResource(R.drawable.ic_marker)

        // --- 지도 배경 클릭 리스너 (선택 해제 로직) ---
        naverMap.setOnMapClickListener { pointF, latLng ->
            Log.d("PlaceBottomSheet", "지도 배경 클릭됨. 선택 해제 시도.")
            // 마커가 있을 경우 제거하고 선택 상태 초기화
            if (currentMarker != null) {
                Log.d("PlaceBottomSheet", "기존 마커 제거 및 선택 해제 실행.")
                currentMarker?.map = null // 지도에서 마커 제거
                currentMarker = null
                selectedLatLng = null
                selectedPlaceName = null
                // 검색창 텍스트 원래대로 복구
                binding.textView20.text = "장소를 입력하세요"
                // binding.textView20.setTextColor(Color.parseColor("#CCCCCC")) // 필요시 색상 복구

                // --- 버튼 비활성화 상태로 업데이트 ---
                updateDoneButtonState(false)
            } else {
                Log.d("PlaceBottomSheet", "지도 배경 클릭됨. 제거할 마커 없음.")
            }
        }

        // --- 심벌 클릭 리스너 설정 (장소 선택 로직) ---
        naverMap.setOnSymbolClickListener { symbol ->
            Log.d("PlaceBottomSheet", "심벌 클릭됨: ${symbol.caption} (${symbol.position.latitude}, ${symbol.position.longitude})")

            // 클릭된 심벌의 좌표와 이름 저장
            val clickedPosition = symbol.position
            // 좌표 유효성 검사
            if (clickedPosition.latitude.isNaN() || clickedPosition.longitude.isNaN()) {
                Log.e("PlaceBottomSheet", "심벌 클릭 시 잘못된 좌표값: $clickedPosition")
                // --- 버튼 상태 변경 없음 (선택 실패) ---
                return@setOnSymbolClickListener false // 기본 동작 막지 않음
            }

            selectedLatLng = clickedPosition
            selectedPlaceName = symbol.caption

            // --- 클릭된 위치에 마커 표시 (오류 수정된 로직) ---
            if (currentMarker == null) {
                Log.d("PlaceBottomSheet", "currentMarker is null. Creating new marker.")
                currentMarker = Marker()

                currentMarker?.icon = customMarkerIcon

                context?.let { ctx -> // context가 null이 아닐 때만 실행
                    try {
                        // colors.xml의 subColor 리소스 사용
                        currentMarker?.iconTintColor = ContextCompat.getColor(ctx, R.color.subColor)
                    } catch (e: Exception) {
                        Log.e("PlaceBottomSheet", "마커 색상 리소스(subColor) 로드 실패", e)
                        // 기본 색상 유지 또는 다른 대체 색상 설정
                    }
                }

                // *** 마커 크기 변경 (픽셀 단위 - 이전과 동일하게 유지 또는 수정) ***
                val desiredWidth = 120 // 예: 80 픽셀 너비
                val desiredHeight = 120 // 예: 100 픽셀 높이
                currentMarker?.width = desiredWidth
                currentMarker?.height = desiredHeight

                currentMarker?.position = clickedPosition // 위치 먼저 설정
                try {
                    currentMarker?.map = naverMap // 지도에 추가
                    Log.d("PlaceBottomSheet", "Marker created and added at symbol position.")
                    // --- 버튼 활성화 상태로 업데이트 ---
                    updateDoneButtonState(true)
                } catch (e: Exception) {
                    Log.e("PlaceBottomSheet", "Error setting map for new marker at symbol click.", e)
                    currentMarker = null // 오류 시 마커 다시 null로
                    // --- 버튼 상태 변경 없음 (마커 추가 실패) ---
                    updateDoneButtonState(false) // 실패 시 비활성화 유지 또는 명시적 비활성화
                }
            } else {
                Log.d("PlaceBottomSheet", "currentMarker exists. Updating position.")
                currentMarker?.position = clickedPosition // 위치 업데이트
                // --- 버튼 활성화 상태로 업데이트 (이미 선택된 상태 유지) ---
                updateDoneButtonState(true)
            }
            // --- 마커 로직 끝 ---

            // 검색창 텍스트를 선택된 장소 이름으로 업데이트
            binding.textView20.text = selectedPlaceName ?: "장소 선택됨"
            // binding.textView20.setTextColor(resources.getColor(R.color.black, null)) // 필요시 텍스트 색상 변경

            // true를 반환하여 기본 동작(정보 창 표시 등)을 막고 이벤트 소비
            true
        }
    }

    // --- MapView 생명주기 관리 ---
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        mapView.onDestroy() // MapView 리소스 정리
        naverMap = null     // 지도 인스턴스 해제
        currentMarker = null
        selectedLatLng = null
        selectedPlaceName = null // 변수 초기화 추가
        _binding = null
        super.onDestroyView()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
    // --- MapView 생명주기 관리 끝 ---

    // Double 값을 표시용으로 포맷하는 헬퍼 함수 (선택 사항)
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    companion object {
        // 인자 키 상수 추가
        private const val ARG_SHOW_AI_BUTTON = "show_ai_button"
        private const val ARG_MEETING_ID = "meeting_id"

        // 기존 메서드 유지 (AI 버튼 표시 안함)
        fun newInstance(): PlaceBottomSheet {
            return PlaceBottomSheet().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_SHOW_AI_BUTTON, false) // 기본적으로 AI 버튼 숨김
                }
            }
        }

        // AI 버튼 표시 여부와 meetingId를 지정할 수 있는 새 메서드
        fun newInstance(showAiButton: Boolean, meetingId: Long = -1): PlaceBottomSheet {
            return PlaceBottomSheet().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_SHOW_AI_BUTTON, showAiButton)
                    putLong(ARG_MEETING_ID, meetingId)
                }
            }
        }
    }
}