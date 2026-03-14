package com.campusgomobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PUT

interface FcmApi {

    @PUT("api/user/fcm-token")
    suspend fun registerToken(@Body body: FcmTokenRequest): Response<FcmTokenResponse>

    @DELETE("api/user/fcm-token")
    suspend fun removeToken(@Body body: FcmTokenRequest): Response<FcmTokenResponse>
}

data class FcmTokenRequest(
    val fcm_token: String,
    val device_id: String? = null
)

data class FcmTokenResponse(
    val message: String? = null
)
