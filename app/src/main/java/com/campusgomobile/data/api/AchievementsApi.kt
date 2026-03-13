package com.campusgomobile.data.api

import com.campusgomobile.data.model.AchievementsResponse
import retrofit2.Response
import retrofit2.http.GET

interface AchievementsApi {

    @GET("api/achievements")
    suspend fun getAchievements(): Response<AchievementsResponse>
}
