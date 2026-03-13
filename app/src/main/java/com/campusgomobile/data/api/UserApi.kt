package com.campusgomobile.data.api

import com.campusgomobile.data.model.ChangePasswordRequest
import com.campusgomobile.data.model.ProfileUpdateResponse
import com.campusgomobile.data.model.User
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.PUT

interface UserApi {

    @GET("api/user")
    suspend fun getUser(): Response<User>

    @PUT("api/user/password")
    suspend fun changePassword(@retrofit2.http.Body request: ChangePasswordRequest): Response<Unit>

    @Multipart
    @POST("api/user/profile")
    suspend fun updateProfile(
        @Part profile_image: MultipartBody.Part? = null,
        @Part("remove_profile_image") removeProfileImage: RequestBody? = null
    ): Response<ProfileUpdateResponse>
}
