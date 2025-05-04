package com.example.maite.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.maite.databinding.ItemPropMeetBinding
import com.example.maite.model.PropMeetItem

interface PropMeetClickListener {
    fun onAcceptClick(item: PropMeetItem)
    fun onRejectClick(item: PropMeetItem)
}

class PropMeetAdapter(private val clickListener: PropMeetClickListener) :
    ListAdapter<PropMeetItem, PropMeetAdapter.PropMeetViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropMeetViewHolder {
        val binding = ItemPropMeetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PropMeetViewHolder(binding, clickListener)
    }

    override fun onBindViewHolder(holder: PropMeetViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class PropMeetViewHolder(
        private val binding: ItemPropMeetBinding,
        private val clickListener: PropMeetClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PropMeetItem) {
            binding.propTitle.text = item.title
            binding.propDate.text = item.date
            binding.propTime.text = item.time
            binding.propPlace.text = item.place

            binding.acceptBtn.setOnClickListener {
                clickListener.onAcceptClick(item)
            }

            binding.rejectBtn.setOnClickListener {
                clickListener.onRejectClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PropMeetItem>() {
        override fun areItemsTheSame(oldItem: PropMeetItem, newItem: PropMeetItem): Boolean {
            return oldItem.title == newItem.title && oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: PropMeetItem, newItem: PropMeetItem): Boolean {
            return oldItem == newItem
        }
    }
}