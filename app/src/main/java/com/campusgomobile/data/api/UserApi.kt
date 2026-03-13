package com.campusgomobile.data.api

import com.campusgomobile.data.model.User
import retrofit2.Response
import retrofit2.http.GET

interface UserApi {

    @GET("api/user")
    suspend fun getUser(): Response<User>
}
