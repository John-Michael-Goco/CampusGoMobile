package com.campusgomobile.data.api

import com.campusgomobile.data.model.ParticipantStatusResponse
import com.campusgomobile.data.model.PlayStateResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ParticipantsApi {

    @GET("api/participants/{participantId}/play")
    suspend fun getPlayState(@Path("participantId") participantId: Int): Response<PlayStateResponse>

    @GET("api/participants/{participantId}/status")
    suspend fun getStatus(@Path("participantId") participantId: Int): Response<ParticipantStatusResponse>

    @POST("api/participants/{participantId}/submit")
    suspend fun submit(
        @Path("participantId") participantId: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<PlayStateResponse>

    @POST("api/participants/{participantId}/quit")
    suspend fun quit(@Path("participantId") participantId: Int): Response<ResponseBody>
}
