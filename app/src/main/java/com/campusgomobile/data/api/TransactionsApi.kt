package com.campusgomobile.data.api

import com.campusgomobile.data.model.TransactionsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TransactionsApi {

    @GET("api/user/transactions")
    suspend fun getTransactions(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("type") type: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<TransactionsResponse>
}
