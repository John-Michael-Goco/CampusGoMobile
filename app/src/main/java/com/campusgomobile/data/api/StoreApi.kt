package com.campusgomobile.data.api

import com.campusgomobile.data.model.RedeemRequest
import com.campusgomobile.data.model.RedeemResponse
import com.campusgomobile.data.model.StoreResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface StoreApi {

    @GET("api/store")
    suspend fun getStore(): Response<StoreResponse>

    @POST("api/store/redeem")
    suspend fun redeem(@Body request: RedeemRequest): Response<RedeemResponse>
}
