package com.campusgomobile.data.model

import com.google.gson.annotations.SerializedName

data class ActivityLogResponse(
    val activity: List<ActivityLogEntry>,
    val pagination: ActivityPagination
)

data class ActivityLogEntry(
    val id: Int,
    @SerializedName("action_key") val actionKey: String,
    val detail: String? = null,
    @SerializedName("display_label") val displayLabel: String? = null,
    val timestamp: String
)

data class ActivityPagination(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("per_page") val perPage: Int,
    val total: Int,
    @SerializedName("last_page") val lastPage: Int
)
