package com.example.maite.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.maite.R
import com.example.maite.model.User
import de.hdodenhof.circleimageview.CircleImageView

class SearchAdapter(private val users: MutableList<User>) : 
    RecyclerView.Adapter<SearchAdapter.UserViewHolder>() {
    
    // Track the currently selected position
    private var selectedPosition = RecyclerView.NO_POSITION
    
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageView: CircleImageView = itemView.findViewById(R.id.ivUserProfile)
        val nameTextView: TextView = itemView.findViewById(R.id.tvUserName)
        val emailTextView: TextView = itemView.findViewById(R.id.tvUserEmail)
        val selectCheckBox: CheckBox = itemView.findViewById(R.id.cbSelectUser)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return UserViewHolder(view)
    }
    
    override fun getItemCount(): Int = users.size
    
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        
        holder.nameTextView.text = user.name
        holder.emailTextView.text = user.email
        holder.selectCheckBox.isChecked = user.isSelected
        
        // Load profile image with Glide
        Glide.with(holder.itemView.context)
            .load(user.profileImageUrl)
            .placeholder(R.drawable.ic_person_placeholder)
            .error(R.drawable.ic_person_placeholder)
            .into(holder.profileImageView)
        
        // Handle checkbox clicks - ensure only one can be checked
        holder.selectCheckBox.setOnClickListener {
            toggleSelection(position)
        }
        
        // Make the entire item clickable to toggle selection
        holder.itemView.setOnClickListener {
            toggleSelection(position)
        }
    }
    
    // Get the selected user (will be a single user or null)
    fun getSelectedUser(): User? {
        return users.find { it.isSelected }
    }
    
    // Update the dataset with new users
    fun updateUsers(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)
        selectedPosition = RecyclerView.NO_POSITION // Reset selection when updating data
        notifyDataSetChanged()
    }
    
    // Toggle selection logic ensuring only one item is selected
    private fun toggleSelection(position: Int) {
        // If the same position is clicked again, don't deselect it
        if (selectedPosition == position && users[position].isSelected) {
            return
        }
        
        // Deselect the previously selected position
        if (selectedPosition != RecyclerView.NO_POSITION) {
            users[selectedPosition].isSelected = false
            notifyItemChanged(selectedPosition)
        }
        
        // Select the new position
        users[position].isSelected = true
        selectedPosition = position
        notifyItemChanged(position)
    }
}