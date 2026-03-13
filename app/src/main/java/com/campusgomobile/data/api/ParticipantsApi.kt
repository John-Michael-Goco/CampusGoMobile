package com.campusgomobile.data.api

import com.campusgomobile.data.model.PlayStateResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ParticipantsApi {

    @GET("api/participants/{participantId}/play")
    suspend fun getPlayState(@Path("participantId") participantId: Int): Response<PlayStateResponse>

    @POST("api/participants/{participantId}/quit")
    suspend fun quit(@Path("participantId") participantId: Int): Response<ResponseBody>
}
