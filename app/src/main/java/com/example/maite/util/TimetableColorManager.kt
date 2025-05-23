package com.example.maite.util

import android.content.Context
import com.example.maite.model.TimetableEntry
import com.example.maite.PreferencesUtil

class TimetableColorManager(private val context: Context) {
    
    private val preferencesUtil = PreferencesUtil(context)
    
    // MAITE 프로젝트에 어울리는 색상 팔레트 (6개) - 더 짙은 색상으로 변경하여 텍스트 가독성 향상
    // 기존 파스텔 톤에서 채도와 명도를 조정한 버전
    private val colorPalette = listOf(
        "#5B7BF5", // 진한 파란색 (기존 d7e3fc에서 변경)
        "#FF8A65", // 진한 복숭아색 (기존 ffc09f에서 변경)
        "#4DB6AC", // 진한 청록색 (기존 95b8d1에서 변경)
        "#81C784", // 진한 연두색 (기존 DCFFFB에서 변경)
        "#F06292", // 진한 분홍색 (기존 f8bbd0에서 변경)
        "#7986CB"  // 진한 보라색 (기존 b2dfdb에서 변경)
    )
    
    // 제목-장소 조합을 키로 하는 색상 매핑을 저장/조회하는 함수들
    private fun getColorMappingKey(title: String, location: String): String {
        return "${title}_${location}"
    }
    
    private fun saveColorMapping(title: String, location: String, colorHex: String) {
        val key = getColorMappingKey(title, location)
        preferencesUtil.setString("color_mapping_$key", colorHex)
    }
    
    private fun getColorMapping(title: String, location: String): String? {
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