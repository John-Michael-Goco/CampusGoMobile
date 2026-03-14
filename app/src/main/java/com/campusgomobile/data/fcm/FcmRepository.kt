package com.campusgomobile.data.fcm

import com.campusgomobile.data.api.FcmApi
import com.campusgomobile.data.api.FcmTokenRequest
import com.campusgomobile.data.auth.TokenStorage
import com.campusgomobile.data.network.NetworkModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.io.IOException

class FcmRepository(
    private val tokenStorage: TokenStorage
) {

    suspend fun registerTokenToBackend(fcmToken: String): Boolean {
        val token = tokenStorage.token.first() ?: return false
        if (token.isBlank()) return false
        return try {
            val client = NetworkModule.createAuthenticatedClient(token)
            val api = NetworkModule.createFcmApi(client)
            val response = api.registerToken(FcmTokenRequest(fcm_token = fcmToken))
            response.isSuccessful
        } catch (_: IOException) {
            false
        } catch (_: retrofit2.HttpException) {
            false
        }
    }

    suspend fun removeTokenFromBackend(fcmToken: String): Boolean {
        val token = tokenStorage.token.first() ?: return false
        if (token.isBlank()) return false
        return try {
            val client = NetworkModule.createAuthenticatedClient(token)
            val api = NetworkModule.createFcmApi(client)
            val response = api.removeToken(FcmTokenRequest(fcm_token = fcmToken))
            response.isSuccessful
        } catch (_: IOException) {
            false
        } catch (_: retrofit2.HttpException) {
            false
        }
    }
}
