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
    
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageView: CircleImageView = itemView.findViewById(R.id.ivUserProfile)
        val nameTextView: TextView = itemView.findViewById(R.id.tvUserName)
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
        holder.selectCheckBox.isChecked = user.isSelected
        
        // Load profile image with Glide
        Glide.with(holder.itemView.context)
            .load(user.profileImageUrl)
            .placeholder(R.drawable.ic_person_placeholder)
            .error(R.drawable.ic_person_placeholder)
            .into(holder.profileImageView)
        
        // Handle checkbox clicks
        holder.selectCheckBox.setOnClickListener {
            user.isSelected = holder.selectCheckBox.isChecked
        }
        
        // Make the entire item clickable to toggle selection
        holder.itemView.setOnClickListener {
            user.isSelected = !user.isSelected
            holder.selectCheckBox.isChecked = user.isSelected
            notifyItemChanged(position)
        }
    }
    
    // Get all selected users
    fun getSelectedUsers(): List<User> {
        return users.filter { it.isSelected }
    }
    
    // Update the dataset with new users
    fun updateUsers(newUsers: List<User>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }
}