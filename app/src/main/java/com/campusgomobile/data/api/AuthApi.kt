package com.campusgomobile.data.api

import com.campusgomobile.data.model.AuthResponse
import com.campusgomobile.data.model.SignInRequest
import com.campusgomobile.data.model.SignUpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApi {

    @POST("api/auth/signin")
    suspend fun signIn(@Body request: SignInRequest): Response<AuthResponse>

    @POST("api/auth/signup")
    suspend fun signUp(@Body request: SignUpRequest): Response<AuthResponse>

    @POST("api/auth/signout")
    suspend fun signOut(@Header("Authorization") authorization: String): Response<SignOutResponse>
}

data class SignOutResponse(val message: String?)
