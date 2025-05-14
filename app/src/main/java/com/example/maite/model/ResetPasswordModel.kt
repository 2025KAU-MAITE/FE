package com.example.maite.model

// DTO for sending reset password code
data class ResetPasswordSendRequest(
    val name: String,
    val email: String,
    val phonenumber: String
)

// DTO for verifying the reset password code
data class ResetPasswordVerifyRequest(
    val phonenumber: String,
    val verificationCode: String
)

// DTO for updating the password
data class ResetPasswordUpdateRequest(
    val email: String,
    val password: String
)

// API response for reset password operations
data class ResetPasswordResponse(
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val result: Any // Can be a String or a ResetPasswordResult object
) {
    // Helper method to get email safely
    fun getEmail(): String {
        return when (result) {
            is ResetPasswordResult -> result.email
            is Map<*,*> -> (result as? Map<*,*>)?.get("email") as? String ?: ""
            else -> ""
        }
    }
    
    // Helper method to get status safely
    fun getStatus(): Boolean {
        return when (result) {
            is ResetPasswordResult -> result.status
            is Map<*,*> -> (result as? Map<*,*>)?.get("status") as? Boolean ?: false
            is String -> true // If result is a string, assume status is true
            else -> false
        }
    }
}

data class ResetPasswordResult(
    val status: Boolean = false,
    val message: String = "",
    val code: String = "",
    val email: String = ""
)
