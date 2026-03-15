package com.campusgomobile.data.api

import com.campusgomobile.data.model.JoinResponse
import com.campusgomobile.data.model.ParticipatingResponse
import com.campusgomobile.data.model.QrResolveResponse
import com.campusgomobile.data.model.QuestDetailResponse
import com.campusgomobile.data.model.QuestHistoryResponse
import com.campusgomobile.data.model.QuestsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface QuestsApi {

    @GET("api/quests")
    suspend fun getQuests(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15
    ): Response<QuestsResponse>

    @GET("api/quests/participating")
    suspend fun getParticipating(): Response<ParticipatingResponse>

    @GET("api/quests/resolve")
    suspend fun resolve(
        @Query("qr") qr: String? = null,
        @Query("quest_id") questId: Int? = null,
        @Query("stage_id") stageId: Int? = null
    ): Response<QrResolveResponse>

    @POST("api/quests/join")
    suspend fun join(@Body body: Map<String, Int>): Response<JoinResponse>

    @GET("api/quests/{questId}")
    suspend fun getQuestDetail(
        @Path("questId") questId: Int,
        @Query("stage") stage: Int? = null,
        @Query("include_questions") includeQuestions: Boolean? = null
    ): Response<QuestDetailResponse>

    @GET("api/quests/history")
    suspend fun getHistory(
        @Query("search") search: String? = null,
        @Query("quest_type") questType: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): Response<QuestHistoryResponse>
}
