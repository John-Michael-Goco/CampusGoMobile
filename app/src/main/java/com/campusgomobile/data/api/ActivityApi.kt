package com.campusgomobile.data.api

import com.campusgomobile.data.model.ActivityLogResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ActivityApi {

    @GET("api/user/activity")
    suspend fun getActivity(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("action") action: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<ActivityLogResponse>
}
