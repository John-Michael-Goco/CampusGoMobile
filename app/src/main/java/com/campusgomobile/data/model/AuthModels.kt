package com.campusgomobile.data.model

import com.google.gson.annotations.SerializedName

// --- Sign in ---
data class SignInRequest(
    val email: String,
    val password: String,
    @SerializedName("device_name") val deviceName: String? = "android"
)

// --- Sign up (register) ---
data class SignUpRequest(
    @SerializedName("student_number") val studentNumber: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    val course: String,
    @SerializedName("year_level") val yearLevel: Int,
    val email: String,
    val password: String,
    @SerializedName("device_name") val deviceName: String? = "android"
)

// --- Auth response (signin & signup) ---
data class AuthResponse(
    val token: String,
    @SerializedName("token_type") val tokenType: String? = "Bearer",
    val user: User
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: String? = null,
    @SerializedName("points_balance") val pointsBalance: Int? = null,
    val level: Int? = null,
    @SerializedName("total_xp_earned") val totalXpEarned: Int? = null,
    @SerializedName("total_completed_quests") val totalCompletedQuests: Int? = null,
    @SerializedName("profile_image") val profileImage: String? = null,
    val student: UserStudent? = null  // optional, from API when role is student
)

data class UserStudent(
    @SerializedName("student_number") val studentNumber: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val course: String? = null,
    @SerializedName("year_level") val yearLevel: Int? = null,
    val section: String? = null
)

// --- Change password ---
data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("password") val password: String,
    @SerializedName("password_confirmation") val passwordConfirmation: String
)

// --- Profile update response (message + user) ---
data class ProfileUpdateResponse(
    val message: String? = null,
    val user: User? = null
)

// --- API error (422 validation) ---
data class ApiErrorBody(
    val message: String? = null,
    val errors: Map<String, List<String>>? = null
)
