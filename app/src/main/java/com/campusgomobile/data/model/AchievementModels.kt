package com.campusgomobile.data.model

import com.google.gson.annotations.SerializedName

data class AchievementsResponse(
    val achievements: List<Achievement>
)

data class Achievement(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerializedName("requirement_type") val requirementType: String? = null,
    @SerializedName("requirement_value") val requirementValue: Int? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    val earned: Boolean = false,
    @SerializedName("earned_at") val earnedAt: String? = null
)
