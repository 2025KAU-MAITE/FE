package com.example.maite.model

data class CreateRoomRequest(
    val name: String,
    val description: String,
    val inviteEmails: List<String>
)