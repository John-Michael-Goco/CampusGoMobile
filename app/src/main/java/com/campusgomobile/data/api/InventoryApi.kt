package com.campusgomobile.data.api

import com.campusgomobile.data.model.InventoryHistoryResponse
import com.campusgomobile.data.model.InventoryResponse
import com.campusgomobile.data.model.UseItemResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface InventoryApi {

    @GET("api/user/inventory")
    suspend fun getInventory(): Response<InventoryResponse>

    @GET("api/user/inventory/history")
    suspend fun getHistory(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): Response<InventoryHistoryResponse>

    @POST("api/user/inventory/{inventoryId}/use")
    suspend fun useItem(@Path("inventoryId") inventoryId: Int): Response<UseItemResponse>
}
