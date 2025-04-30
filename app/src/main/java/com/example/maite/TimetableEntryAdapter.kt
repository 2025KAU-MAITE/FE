package com.example.maite.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.maite.R
import com.example.maite.model.TimetableEntry

class TimetableEntryAdapter(
    private val entries: List<TimetableEntry>,
    private val onItemClick: (TimetableEntry) -> Unit
) : RecyclerView.Adapter<TimetableEntryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_entry_title)
        val tvDay: TextView = view.findViewById(R.id.tv_entry_day)
        val tvTime: TextView = view.findViewById(R.id.tv_entry_time)
        val tvLocation: TextView = view.findViewById(R.id.tv_entry_location)
        val colorIndicator: View = view.findViewById(R.id.color_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timetable_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]

        // 요일 변환
        val days = arrayOf("", "월", "화", "수", "목", "금", "토", "일")
        val dayText = if (entry.dayOfWeek in 1..7) days[entry.dayOfWeek] else ""

        holder.tvTitle.text = entry.title
        holder.tvDay.text = "${dayText}요일"
        holder.tvTime.text = "${formatHour(entry.startHour)}:00 ~ ${formatHour(entry.endHour)}:00"
        holder.tvLocation.text = entry.location.takeIf { it.isNotEmpty() } ?: "장소 없음"
        holder.colorIndicator.setBackgroundColor(Color.parseColor(entry.colorHex))

        // 클릭 이벤트
        holder.itemView.setOnClickListener {
            onItemClick(entry)
        }
    }

    private fun formatHour(hour: Int): String {
        return String.format("%02d", hour)
    }

    override fun getItemCount() = entries.size
}