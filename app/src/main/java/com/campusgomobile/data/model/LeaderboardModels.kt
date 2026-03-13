package com.campusgomobile.data.model

import com.google.gson.annotations.SerializedName

data class LeaderboardResponse(
    val entries: List<LeaderboardEntry>,
    val period: String,
    val periods: List<String>? = null,
    @SerializedName("value_label") val valueLabel: String? = null,
    @SerializedName("my_rank") val myRank: Int? = null,
    @SerializedName("my_value") val myValue: Int? = null
)

data class LeaderboardEntry(
    val rank: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("user_name") val userName: String,
    val value: Int
)
