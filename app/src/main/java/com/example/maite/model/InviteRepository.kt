package com.example.maite.model

class InviteRepository {

    fun getInviteList(): List<InviteListItem> {
        // Dummy data for demonstration
        return listOf(
            InviteListItem("김정훈"),
            InviteListItem("안성진"),
            InviteListItem("이민우"),
            InviteListItem("박지원")
        )
    }

    // Method to add new invites - can be implemented later
    fun addInvite(inviteListItem: InviteListItem): List<InviteListItem> {
        // In a real app, this would interact with a database or network
        // For now, we just simulate adding to the list
        val currentList = getInviteList().toMutableList()
        currentList.add(inviteListItem)
        return currentList
    }
}