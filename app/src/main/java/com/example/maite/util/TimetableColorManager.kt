package com.example.maite.util

import android.content.Context
import com.example.maite.model.TimetableEntry
import com.example.maite.PreferencesUtil

class TimetableColorManager(private val context: Context) {
    
    private val preferencesUtil = PreferencesUtil(context)
    
    // MAITE 프로젝트에 어울리는 색상 팔레트 (8개) - 팀원 제공 색상에서 짙은 색상 위주 선택
    // 텍스트 가독성을 고려하여 각 팔레트에서 가장 짙은 색상들로 구성
    private val colorPalette = listOf(
        "#809bce", // Honeydew & Cerulean - 짙은 파스텔 블루 (가장 짙음)
        "#ffc09f", // Peach & Lemon - 따뜻한 복숭아색
        "#a0ced9", // Peach & Lemon - 차분한 청록색  
        "#abc4ff", // Alice Blue & Lavender - 선명한 라벤더 블루
        "#95b8d1", // Honeydew & Cerulean - 부드러운 하늘색
        "#DABFDE", // Pastel Rainbow - 세련된 라벤더 퍼플
        "#b2dfdb", // Pink & Mint - 은은한 민트 그린
        "#f8bbd0"  // Pink & Mint - 온화한 핑크 (가장 연하지만 포인트용)
    )
    
    // 제목-장소 조합을 키로 하는 색상 매핑을 저장/조회하는 함수들
    private fun getColorMappingKey(title: String, location: String): String {
        return "${title}_${location}"
    }
    
    private fun saveColorMapping(title: String, location: String, colorHex: String) {
        val key = getColorMappingKey(title, location)
        preferencesUtil.setString("color_mapping_$key", colorHex)
    }
    
    // Repository에서도 사용할 수 있도록 public으로 변경
    fun getColorMapping(title: String, location: String): String? {
        val key = getColorMappingKey(title, location)
        return preferencesUtil.getString("color_mapping_$key")
    }
    
    // 다음 사용할 색상 인덱스를 저장/조회
    private fun getNextColorIndex(): Int {
        return preferencesUtil.getInt("next_color_index", 0)
    }
    
    private fun saveNextColorIndex(index: Int) {
        preferencesUtil.setInt("next_color_index", index % colorPalette.size)
    }
    
    /**
     * 시간표 항목에 색상을 배정하는 메인 함수
     * - 기존에 같은 제목+장소 조합이 있으면 같은 색상 사용
     * - 새로운 조합이면 순차적으로 다음 색상 배정
     */
    fun assignColor(entry: TimetableEntry): TimetableEntry {
        // 기존에 같은 제목+장소 조합이 있는지 확인
        val existingColor = getColorMapping(entry.title, entry.location)
        
        return if (existingColor != null) {
            // 기존 색상이 있으면 재사용
            entry.copy(colorHex = existingColor)
        } else {
            // 새로운 조합이면 다음 색상 배정
            val nextIndex = getNextColorIndex()
            val assignedColor = colorPalette[nextIndex]
            
            // 색상 매핑 저장
            saveColorMapping(entry.title, entry.location, assignedColor)
            
            // 다음 인덱스 저장 (순환)
            saveNextColorIndex(nextIndex + 1)
            
            entry.copy(colorHex = assignedColor)
        }
    }
    
    /**
     * 여러 시간표 항목에 일괄적으로 색상 배정
     */
    fun assignColors(entries: List<TimetableEntry>): List<TimetableEntry> {
        return entries.map { assignColor(it) }
    }
    
    /**
     * 색상 매핑 초기화 (시간표 초기화 시 사용)
     */
    fun clearColorMappings() {
        // 모든 color_mapping_ 키 삭제
        val editor = preferencesUtil.getSharedPreferences().edit()
        val allKeys = preferencesUtil.getSharedPreferences().all.keys
        
        allKeys.forEach { key ->
            if (key.startsWith("color_mapping_")) {
                editor.remove(key)
            }
        }
        
        // 다음 색상 인덱스도 초기화
        editor.putInt("next_color_index", 0)
        editor.apply()
    }
    
    /**
     * 사용 가능한 색상 팔레트 반환 (디버깅 또는 UI에서 미리보기 용도)
     */
    fun getColorPalette(): List<String> {
        return colorPalette.toList()
    }
    
    /**
     * 현재 색상 인덱스 반환 (디버깅 용도)
     */
    fun getCurrentColorIndex(): Int {
        return getNextColorIndex()
    }
}