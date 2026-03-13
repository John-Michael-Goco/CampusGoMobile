package com.campusgomobile.data.api

import com.campusgomobile.data.model.StudentSearchResponse
import com.campusgomobile.data.model.TransferRequest
import com.campusgomobile.data.model.TransferResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PointsTransferApi {

    @GET("api/students/search")
    suspend fun searchStudent(
        @Query("student_id") studentId: String
    ): Response<StudentSearchResponse>

    @POST("api/points/transfer")
    suspend fun transfer(
        @Body request: TransferRequest
    ): Response<TransferResponse>
}
