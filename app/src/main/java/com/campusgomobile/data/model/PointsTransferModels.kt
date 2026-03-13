package com.campusgomobile.data.model

import com.google.gson.annotations.SerializedName

/** Response from GET /api/students/search */
data class StudentSearchResponse(
    val student: TransferStudent
)

/** Student found by school_id, used as recipient for transfer */
data class TransferStudent(
    val id: Int,
    val name: String,
    val email: String? = null,
    @SerializedName("school_id") val schoolId: String,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val course: String? = null,
    @SerializedName("year_level") val yearLevel: Int? = null,
    val section: String? = null,
    @SerializedName("points_balance") val pointsBalance: Int? = null,
    @SerializedName("profile_image") val profileImage: String? = null
)

/** Request body for POST /api/points/transfer */
data class TransferRequest(
    @SerializedName("to_user_id") val toUserId: Int,
    val amount: Int
)

/** Response from POST /api/points/transfer */
data class TransferResponse(
    val message: String? = null,
    @SerializedName("points_balance") val pointsBalance: Int,
    val transferred: TransferredInfo? = null
)

data class TransferredInfo(
    @SerializedName("to_user_id") val toUserId: Int,
    @SerializedName("to_name") val toName: String,
    val amount: Int
)
