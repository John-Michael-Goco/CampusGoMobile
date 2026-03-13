package com.campusgomobile.data.api

import com.campusgomobile.data.model.LeaderboardResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface LeaderboardApi {

    @GET("api/leaderboard")
    suspend fun getLeaderboard(
        @Query("period") period: String = "week"
    ): Response<LeaderboardResponse>
}
